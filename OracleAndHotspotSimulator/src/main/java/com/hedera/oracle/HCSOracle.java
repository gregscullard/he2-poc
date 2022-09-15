package com.hedera.oracle;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.proto.Report;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Log4j2
public class HCSOracle implements Runnable {
    // thread management
    private boolean doStop = false;
    private final PrivateKey privateKey;
    private final AccountId accountId;
    private final String network;
    private final TopicId topicId;
    private final Client client;

    private final TokenId tokenId;

    private long epochSeconds = 10;
    private int minEpochReports = 2;
    ConcurrentHashMap<Long, Map<Integer, Integer>> beaconReports = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Map<Long, Integer>> beaconReportsHistory = new ConcurrentHashMap<>();
    long currentEpoch = 0;
    private final Map<Integer, AccountId> hotspotsById;

    // Hedera client
    public HCSOracle(PrivateKey privateKey, AccountId accountId, String network, TopicId topicId, TokenId tokenId, Map<Integer, AccountId> hotspotsById) throws Exception {
        this.privateKey = privateKey;
        this.accountId = accountId;
        this.network = network;
        this.topicId = topicId;
        this.tokenId = tokenId;
        this.hotspotsById = hotspotsById;

        this.client = Client.forName(this.network);
        this.client.setOperator(accountId, privateKey);
        this.client.setMaxNodeAttempts(1);
        this.client.setMaxAttempts(1);
    }

    public synchronized void stop() {
        this.doStop = true;
    }

    private synchronized boolean keepRunning() {
        return this.doStop == false;
    }

    public void run() {
        log.info("HCS Oracle running");

        CompletionHandler completionHandler = new CompletionHandler();

        BiConsumer<Throwable, TopicMessage> errorHandler = (error, message) -> {
            log.error(error);
        };

        Predicate<Throwable> retryHandler = (error) -> {
            log.info("Retry Handler invoked");
            return true;
        };

        //TODO: Subscribe from last process message
        //TODO: Persist beacon reports
        new TopicMessageQuery()
                .setTopicId(topicId)
                .setCompletionHandler(completionHandler)
                .setErrorHandler(errorHandler)
                .setRetryHandler(retryHandler)
                .subscribe(client, message -> {
                    this.messageHandler(message);
                });
    }

    // handles reports
    // counts number of reports by each hotspots in an "epoch"
    // if the reports in the epoch are >= minEpochReports, a reward is paid
    // for example, if the epoch is 1 day and minEpochReports is 24, then
    // a reward would be paid if the hotspot sent a report more than 24 times.
    private void messageHandler(TopicMessage message) {
        log.debug("Got HCS Message");
        try {
            Report report = Report.parseFrom(message.contents);
            Map<Integer, Integer> epochReports = new HashMap<>();
            int hotspotId = 0;
            // is it a beacon report ?
            if (report.hasBeaconReport()) {
                // beacon report
                Instant reportTimestamp = message.consensusTimestamp;
                hotspotId = report.getBeaconReport().getId();

                log.debug("beacon report {}", reportTimestamp.getEpochSecond());
                // construct an identifier for this report
                //TODO: calculate epoch properly
                long timestampSeconds = reportTimestamp.getEpochSecond();
                Map<Long, Integer> beacon = new HashMap<>();
                if (beaconReportsHistory.containsKey(hotspotId)) {
                    beacon = beaconReportsHistory.get(hotspotId);
                }
                if (beacon.containsKey(timestampSeconds)) {
                    int count = beacon.get(timestampSeconds).intValue() + 1;
                    beacon.put(timestampSeconds, count);
                } else {
                    beacon.put(timestampSeconds, 1);
                }
                beaconReportsHistory.put(hotspotId, beacon);

                log.debug("epochSeconds is {}", epochSeconds);
                long reportEpoch = timestampSeconds / epochSeconds;
                if (currentEpoch == 0) {
                    currentEpoch = reportEpoch;
                }
                //<epoch, <hotspot, reportsCount>>
                if (beaconReports.containsKey(reportEpoch)) {
                    // we have reports for this epoch
                    epochReports = beaconReports.get(currentEpoch);
                    if (epochReports.containsKey(hotspotId)) {
                        // there is at least one report for this hotspot
                        int reportCount = epochReports.get(hotspotId).intValue() + 1;
                        epochReports.put(hotspotId, reportCount);
                    } else {
                        // create a new report counter for the hotspot
                        epochReports.put(hotspotId, 1);
                    }
                } else {
                    // new epoch
                    epochReports.put(hotspotId, 1);
                }
                beaconReports.put(reportEpoch, epochReports);

                if (currentEpoch != reportEpoch) {
                    log.debug("minEpochReports is {}", minEpochReports);
                    // new epoch, pay hotspots for their reports
                    if (beaconReports.containsKey(currentEpoch)) {
                        epochReports = beaconReports.get(currentEpoch);
                        for (Map.Entry<Integer, Integer> reports : epochReports.entrySet()) {
                            hotspotId = reports.getKey();
                            int reportCount = reports.getValue();
                            if (reports.getValue() >= minEpochReports) {
                                log.info("*** Epoch ({}) Rewarding hotspot {} for {} reports", currentEpoch, hotspotId, reportCount);
                                try {
                                    issueBeaconReward(hotspotId);
                                } catch (PrecheckStatusException e) {
                                    log.error(e);
                                } catch (TimeoutException e) {
                                    log.error(e);
                                }
                            } else {
                                log.info("!!! Epoch ({}) Hotspot {} did not beacon sufficiently ({} of {})", currentEpoch, hotspotId, reportCount, minEpochReports);
                            }
                        }
                    }
                    // clear the reports count for this epoch from the map
                    beaconReports.remove(currentEpoch);
                    currentEpoch = reportEpoch;
                }
            } else if (report.hasWitnessReport()) {
                // witness report
                Instant reportTimestamp = message.consensusTimestamp;
                log.debug("witness report {}", reportTimestamp.getEpochSecond());
            }
        } catch (InvalidProtocolBufferException e) {
            log.error(e);
        }
    }

    //TODO: Run as a thread ?
    private void issueBeaconReward(int hotspotId) throws PrecheckStatusException, TimeoutException {
        new TransferTransaction()
            .addTokenTransfer(this.tokenId, accountId, -1)
            .addTokenTransfer(this.tokenId, hotspotsById.get(hotspotId), 1)
            .execute(client);
    }
    public synchronized void setEpochDuration(int seconds) {
        log.info("Oracle set epoch duration {}", seconds);
        this.epochSeconds = seconds;
    }
    public synchronized void setMinEpochReports(int minReports) {
        log.info("Oracle set min beacon reports per epoch {}", minReports);
        this.minEpochReports = minReports;
    }
    public JsonObject getDetails() {
        JsonObject details = new JsonObject();
        details.put("epochDuration", epochSeconds);
        details.put("minEpochReports", minEpochReports);
        details.put("currentEpoch", currentEpoch);
        return details;
    }
    public Map<Long, Integer> getBeaconHistory(int id) {
        if (beaconReportsHistory.containsKey(id)) {
            return beaconReportsHistory.get(id);
        } else {
            return new HashMap<>();
        }
    }
}
