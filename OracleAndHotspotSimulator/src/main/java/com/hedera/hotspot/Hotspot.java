package com.hedera.hotspot;

import com.hedera.hashgraph.sdk.*;
import com.hedera.proto.BeaconReport;
import com.hedera.proto.Report;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final String region;
    // beacon report
    private final Report report;
    // Reporting topicId
    private final TopicId topicId;
    private long startSeconds = 0;
    private long reportCount = 1;

    public Hotspot(int id, String region, TopicId topicId,  PrivateKey privateKey, AccountId accountId, String network) {
        this.id = id;
        this.region = region;
        BeaconReport beaconReport = BeaconReport.newBuilder()
                .setId(this.id)
                .setRegion(this.region)
                .build();
        this.report = Report.newBuilder()
                .setBeaconReport(beaconReport)
                .build();
        this.topicId = topicId;
        this.privateKey = privateKey;
        this.accountId = accountId;
        this.network = network;
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
        while(keepRunning()) {
            for (Map.Entry<String, AccountId> entry : nodes.entrySet()) {
                try {
                    new TopicMessageSubmitTransaction()
                        .setNodeAccountIds(List.of(entry.getValue()))
                        .setTopicId(topicId)
                        .setMessage(report.toByteString())
                        .execute(client);
                    log.debug("Hotspot {} report {} submitted", id, reportCount);
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

