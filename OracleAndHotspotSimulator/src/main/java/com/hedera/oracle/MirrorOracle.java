package com.hedera.oracle;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.Utils;
import com.hedera.hashgraph.sdk.*;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import com.hedera.proto.Report;

@Log4j2
public class MirrorOracle implements Runnable {
    // thread management
    private boolean doStop = false;
    private final int intervalMs = 1500;
    private final String mirrorUrl;
    private final PrivateKey privateKey;
    private final AccountId accountId;
    private final String network;
    private final TopicId topicId;

    // Hedera client
    public MirrorOracle(PrivateKey privateKey, AccountId accountId, String network, TopicId topicId) throws Exception {
        this.privateKey = privateKey;
        this.accountId = accountId;
        this.network = network;
        this.topicId = topicId;

        switch (network.toUpperCase()) {
            case "MAINNET":
                mirrorUrl = "https://mainnet-public.mirrornode.hedera.com";
                break;
            case "TESTNET":
                mirrorUrl = "https://35.239.255.55";
                break;
            case "PREVIEWNET":
                mirrorUrl = "https://previewnet.mirrornode.hedera.com";
                break;
            default:
                log.error("Invalid network {} provided to Oracle", network);
                throw new Exception("Invalid network provided to Oracle");
        }
    }

    public synchronized void stop() {
        this.doStop = true;
    }

    private synchronized boolean keepRunning() {
        return this.doStop == false;
    }

    public void run() {
        log.info("Oracle running");
        Client client = Client.forName(this.network);
        client.setOperator(accountId, privateKey);
        client.setMaxNodeAttempts(1);
        client.setMaxAttempts(1);
//        String nextTimestamp = String.valueOf(Instant.now().getEpochSecond());
        String nextTimestamp = "0.0";

        String uri = "/api/v1/topics/".concat(this.topicId.toString()).concat("/messages");
        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (this.keepRunning()) {
            try {

                log.debug("Checking for messages on topic id {}", this.topicId.toString());

                Map<String, String> queryParameters = new HashMap<>();
                queryParameters.put("timestamp", "gt:".concat(nextTimestamp));
                queryParameters.put("order", "asc");
                queryParameters.put("encoding", "base64");
                queryParameters.put("limit", "100");

                Future<JsonObject> future = executor.submit(Utils.queryMirror(this.mirrorUrl, uri, queryParameters));

                try {
                    JsonObject response = future.get();
                    if (response != null) {
                        MirrorTopicMessages mirrorTopicMessages = response.mapTo(MirrorTopicMessages.class);
                        int messagesCount = mirrorTopicMessages.messages.size();
                        if (messagesCount > 0) {
                            nextTimestamp = mirrorTopicMessages.messages.get(messagesCount - 1).consensusTimestamp;
                        }
                        handle(mirrorTopicMessages);
                    }
                } catch (InterruptedException e) {
                    log.error(e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error(e);
                }
            } catch (RuntimeException e) {
                log.error(e);
            }

            if (keepRunning()) {
                // only sleep if necessary
                try {
                    Thread.sleep(this.intervalMs);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        }
        try {
            client.close();
        } catch (TimeoutException e) {
            log.error(e);
        }
        executor.shutdown();
    }

    public void handle(MirrorTopicMessages mirrorTopicMessages) {
        for (MirrorTopicMessage mirrorTopicMessage : mirrorTopicMessages.messages) {
            log.debug("Got HCS Message");
            ByteString mirrorMessageData = mirrorTopicMessage.message();
            try {
                Report report = Report.parseFrom(mirrorMessageData);
                // is it a beacon report ?
                if (report.hasBeaconReport()) {
                    // beacon report
                    log.debug("beacon report");
                } else if (report.hasWitnessReport()) {
                    // witness report
                    log.debug("witness report");
                }
            } catch (InvalidProtocolBufferException e) {
                log.error(e);
            }
        }
    }
}
