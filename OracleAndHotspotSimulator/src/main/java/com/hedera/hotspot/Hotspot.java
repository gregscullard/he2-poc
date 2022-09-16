package com.hedera.hotspot;

import com.hedera.hashgraph.sdk.*;
import com.hedera.proto.BeaconReport;
import com.hedera.proto.Report;
import com.hedera.proto.WitnessReport;
import com.hedera.yamlconfig.YamlConfigManager;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.ArrayList;
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
    private PrivateKey privateKey;
    private AccountId accountId;
    private String network;
    // beacon properties
    private final int id;
    private final String name;
    // Reporting topicId
    private final TopicId topicId;
    private long startSeconds = 0;
    private long reportCount = 1;
    private List<AccountId> paidAccounts = new ArrayList<>();
    private final Hotspots hotspots;
    private final Random random;

    public Hotspot(int id, String name, TopicId topicId, PrivateKey privateKey, AccountId accountId, String network, List<String> paidAccounts, Hotspots hotspots) {
        this.id = id;
        this.name = name;
        this.topicId = topicId;
        this.privateKey = privateKey;
        this.accountId = accountId;
        this.network = network;
        if (paidAccounts.size() == 0) {
            this.paidAccounts.add(this.accountId);
        } else {
            for (int i=0; i < paidAccounts.size(); i++) {
                this.paidAccounts.add(AccountId.fromString(paidAccounts.get(i)));
            }
        }
        this.hotspots = hotspots;
        this.random = new Random();
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
        return this.doStop == false;
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
                    String reportType = "";
                    Report.Builder report = Report.newBuilder();
                    int countHotspots = hotspots.hotspotCount();
                    if (beacon || (countHotspots == 0)) {
                        // beacon if alone
                        reportType = "beacon";
                        BeaconReport beaconReport = BeaconReport.newBuilder()
                                .setId(this.id)
                                .setName(this.name)
                                .build();
                        report.setBeaconReport(beaconReport)
                                .build();
                    } else {
                        // witness randomly
                        reportType = "witness";
                        int witnessedId = random.nextInt(countHotspots);
                        if (witnessedId == this.id) {
                            if (this.id == 0) {
                                witnessedId = 1;
                            } else {
                                witnessedId = witnessedId - 1;
                            }
                        }
                        WitnessReport witnessReport = WitnessReport.newBuilder()
                                .setWitnessingId(this.id)
                                .setWitnessedId(witnessedId)
                                .build();
                        report.setWitnessReport(witnessReport);
                    }
                    //TODO: Make this dynamic ?
                    beacon = !beacon;

                    new TopicMessageSubmitTransaction()
                        .setNodeAccountIds(List.of(entry.getValue()))
                        .setTopicId(topicId)
                        .setMessage(report.build().toByteString())
                        .execute(client);
                    log.debug("Hotspot {} {} report {} submitted", id, reportType, reportCount);
                    reportCount++;
                    //TODO: not waiting for receipt in this POC so fewer threads
                    //can submit more transactions to the network faster
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    log.error(e, e);
                } catch (PrecheckStatusException e) {
                    log.error(e, e);
                } catch (TimeoutException e) {
                    log.error(e, e);
                }
                if (!keepRunning()) {
                    break;
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

