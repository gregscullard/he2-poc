package com.hedera.hotspot;

import com.hedera.hashgraph.sdk.*;
import com.hedera.json.JsonConstants;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

@Log4j2
public class Hotspot implements Runnable {
    // thread management
    private boolean doStop = false;
    private int intervalMs = 1000;
    // Hedera client
    private final PrivateKey privateKey;
    private final AccountId accountId;
    private final String network;
    // beacon properties
    private final int id;
    // Reporting topicId
    private final TopicId topicId;
    private long startSeconds;
    private long reportCount = 1;
    private final Hotspots hotspots;
    private final Random random;
    private final int configuredHotspots;

    public Hotspot(int id, String name, TopicId topicId, PrivateKey privateKey, AccountId accountId, String network, List<String> paidAccounts, int configuredHotspots, Hotspots hotspots) throws FileNotFoundException, PrecheckStatusException, TimeoutException {
        this.hotspots = hotspots;
        this.id = id;
        this.topicId = topicId;
        this.privateKey = privateKey;
        this.accountId = accountId;
        this.network = network;
        this.random = new Random();
        this.configuredHotspots = configuredHotspots;

        // advertise yourself on HCS
        Client client = Client.forName(network);
        client.setOperator(accountId, privateKey);

//        com.hedera.proto.Hotspot hotspot = com.hedera.proto.Hotspot.newBuilder()
//                .setId(this.id)
//                .setAccountId(this.accountId.toString())
//                .setName(name)
//                .addAllAccountIds(paidAccounts)
//                .build();
//
//        Report report = Report.newBuilder()
//                .setHotspot(hotspot)
//                .build();

        if (paidAccounts.size() == 0) {
            paidAccounts.add(accountId.toString());
        }
        JsonObject jsonReport = new JsonObject();
        jsonReport.put(JsonConstants.ID, this.id);
        jsonReport.put(JsonConstants.ACCOUNT_ID, this.accountId.toString());
        jsonReport.put(JsonConstants.NAME, name);
        jsonReport.put(JsonConstants.PAID_ACCOUNT_IDS, paidAccounts);
        JsonObject json = JsonObject.mapFrom(jsonReport);
        new TopicMessageSubmitTransaction()
                .setTopicId(topicId)
                .setMessage(json.encode())
                .execute(client);

        client.close();
    }

    public JsonObject getDetails() {
        long runSeconds = Instant.now().getEpochSecond() - this.startSeconds;
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("startSeconds", this.startSeconds);
        jsonObject.put("runSeconds", runSeconds);
        jsonObject.put("reportCount", this.reportCount);
        jsonObject.put("intervalMs", this.intervalMs);
        return jsonObject;
    }

    public synchronized void stop() {
        this.doStop = true;
        log.info("Hotspot {} stopping", this.id);
    }

    public synchronized void setIntervalMs(int intervalMs) {
        log.info("Hotspot ID {} reporting at {} ms interval", this.id, intervalMs);
        this.intervalMs = intervalMs;
    }
    private synchronized boolean keepRunning() {
        return !this.doStop;
    }

    public void run() {

        this.startSeconds = Instant.now().getEpochSecond();
        log.info("Hotspot ID {} reporting at {} ms interval", this.id, intervalMs);
        Client client = Client.forName(network);
        client.setOperator(accountId, privateKey);
        client.setMaxNodeAttempts(1);
        client.setMaxAttempts(1);
        Map<String, AccountId> nodes = client.getNetwork();
        boolean beacon = true;
        while(keepRunning()) {
            for (Map.Entry<String, AccountId> entry : nodes.entrySet()) {
                try {
                    String reportType;
//                    Report.Builder report = Report.newBuilder();
                    JsonObject jsonReport = new JsonObject();
                    jsonReport.put(JsonConstants.ID, this.id);
                    log.debug("Enabled ids {}", this.hotspots.getEnabledIds());
                    if (beacon || (this.hotspots.getEnabledIds().size() == 1)) {
                        // beacon if alone
                        reportType = "beacon";
//                        BeaconReport beaconReport = BeaconReport.newBuilder()
//                                .setId(this.id)
////                                .setName(this.name)
//                                .build();
//                        report.setBeaconReport(beaconReport)
//                                .build();
                        log.debug("----> Beacon {}", this.id);
                    } else {
                        // witness randomly
                        reportType = "witness";
                        log.debug("enabled ids size {}", this.hotspots.getEnabledIds());
                        int hotspotIdToWitness = random.nextInt(this.hotspots.getEnabledIds().size());
                        int witnessedId = this.hotspots.getEnabledIds().get(hotspotIdToWitness);
                        if (witnessedId == this.id) {
                            if (hotspotIdToWitness == 0) {
                                // can't go below index 1, take the last from the list
                                witnessedId = this.hotspots.getEnabledIds().get(this.hotspots.getEnabledIds().size() -1);
                            } else {
                                // go back one
                                witnessedId = this.hotspots.getEnabledIds().get(hotspotIdToWitness - 1);
                            }
                        }
                        jsonReport.put(JsonConstants.WITNESSED_ID, witnessedId);
//                        WitnessReport witnessReport = WitnessReport.newBuilder()
//                                .setWitnessingId(this.id)
//                                .setWitnessedId(witnessedId)
//                                .build();
//                        report.setWitnessReport(witnessReport);
                        log.debug("----> Witness from {} to {}", this.id, witnessedId);
                    }
                    //TODO: Make this dynamic ?
                    beacon = !beacon;

                    JsonObject json = JsonObject.mapFrom(jsonReport);
                    new TopicMessageSubmitTransaction()
                        .setNodeAccountIds(List.of(entry.getValue()))
                        .setTopicId(topicId)
                        .setMessage(json.encode())
                        .execute(client);
                    log.debug("Hotspot {} {} report #{} submitted", id, reportType, reportCount);
                    reportCount++;
                    //TODO: not waiting for receipt in this POC so fewer threads
                    //can submit more transactions to the network faster
                    Thread.sleep(intervalMs);
                } catch (InterruptedException | PrecheckStatusException | TimeoutException e) {
                    log.error(e);
                }
                if (!keepRunning()) {
                    break;
                }
                long runSeconds = Instant.now().getEpochSecond() - this.startSeconds;
                if ((runSeconds > 60) && (this.id > this.configuredHotspots)) {
                    // stop running after 1 minute
                    this.hotspots.remove(this.id);
                }
            }
        }
        try {
            client.close();
        } catch (TimeoutException e) {
            log.error(e, e);
        }
        log.info("Hotspot {} stopped", this.id);
    }
}

