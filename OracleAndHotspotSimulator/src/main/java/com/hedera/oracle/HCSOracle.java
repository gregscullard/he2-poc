package com.hedera.oracle;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.proto.Report;
import com.hedera.yamlconfig.YamlConfigManager;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

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
    private int minWitnessReports;
    ConcurrentHashMap<Integer, HotspotReports> hotspotReportsMap = new ConcurrentHashMap<>();
    long currentEpoch = 0;
    long currentEpochStart = 0;
    private final Map<Integer, AccountId> hotspotsById;

    // Hedera client
    public HCSOracle(YamlConfigManager yamlConfigManager, String network) throws Exception {
        this.privateKey = PrivateKey.fromString(yamlConfigManager.getTreasuryAccountKey());
        this.accountId = AccountId.fromString(yamlConfigManager.getTreasuryAccount());
        this.network = network;
        this.topicId = TopicId.fromString(yamlConfigManager.getTopicId());
        this.tokenId = TokenId.fromString(yamlConfigManager.getTokenId());
        this.hotspotsById = yamlConfigManager.getHotspotIdAccountMap();

        this.minEpochReports = yamlConfigManager.getOracleMinEpochReports();
        this.epochSeconds = yamlConfigManager.getOracleEpochSeconds();
        this.minWitnessReports = yamlConfigManager.getOracleMinWitnessReports();

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
                // check if there is a report already for this hotspot
                HotspotReports hotspotReports = new HotspotReports();
                if (hotspotReportsMap.containsKey(hotspotId)) {
                    hotspotReports = hotspotReportsMap.get(hotspotId);
                }

                log.debug("beacon report {}", reportTimestamp.getEpochSecond());
                // construct an identifier for this report
                //TODO: calculate epoch properly
                long timestampSeconds = reportTimestamp.getEpochSecond();
                log.debug("epochSeconds is {}", epochSeconds);
                long reportEpoch = timestampSeconds / epochSeconds;
                if (currentEpoch == 0) {
                    currentEpoch = reportEpoch;
                    currentEpochStart = timestampSeconds;
                }
                // check the current epoch is still the same, if different issue payments
                if (currentEpoch != reportEpoch) {
                    log.debug("minEpochReports is {}", minEpochReports);
                    // new epoch, pay hotspots for their reports
                    for (Map.Entry<Integer, HotspotReports> reports : hotspotReportsMap.entrySet()) {
                        HotspotReports reportsToCheck = reports.getValue();
                        HotspotReportsCounter countersToCheck = reportsToCheck.getReportByEpoch(currentEpoch, currentEpochStart);
                        int reportCount = countersToCheck.getBeaconCount();
                        if (reportCount >= minEpochReports) {
                            log.info("*** Epoch ({}) Rewarding hotspot {} for {} reports", currentEpoch, hotspotId, reportCount);
                            try {
                                long payment = 1;
                                issueBeaconReward(reports.getKey(), payment);
                                // update the counters to show payment
                                countersToCheck.setRewardPaid(payment);
                                reportsToCheck.updateReportsCounter(currentEpoch, countersToCheck);
                                hotspotReportsMap.put(reports.getKey(), reportsToCheck);
                            } catch (PrecheckStatusException e) {
                                log.error(e);
                            } catch (TimeoutException e) {
                                log.error(e);
                            }
                        } else {
                            log.info("!!! Epoch ({}) Hotspot {} did not beacon sufficiently ({} of {})", currentEpoch, reports.getKey(), reportCount, minEpochReports);
                        }
                    }
                    currentEpoch = reportEpoch;
                    currentEpochStart = timestampSeconds;
                }

                HotspotReportsCounter hotspotReportsCounter = hotspotReports.getReportByEpoch(reportEpoch, currentEpochStart);
                hotspotReportsCounter.addBeaconCount();
                hotspotReports.updateReportsCounter(reportEpoch, hotspotReportsCounter);
                hotspotReportsMap.put(hotspotId, hotspotReports);
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
    private void issueBeaconReward(int hotspotId, long quantity) throws PrecheckStatusException, TimeoutException {
        new TransferTransaction()
            .addTokenTransfer(this.tokenId, accountId, -quantity)
            .addTokenTransfer(this.tokenId, hotspotsById.get(hotspotId), quantity)
            .execute(client);
    }
    public synchronized void setEpochDuration(int seconds) {
        log.info("Oracle set epoch duration {}", seconds);
        this.epochSeconds = seconds;
    }
    public synchronized void setMinWitnessReports(int minReports) {
        log.info("Oracle set min witness reports per epoch {}", minReports);
        this.minWitnessReports = minReports;
    }

    public synchronized void setMinBeaconReports(int minReports) {
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
    public HotspotReports getBeaconHistory(int id) {
        if (hotspotReportsMap.containsKey(id)) {
            return hotspotReportsMap.get(id);
        } else {
            return new HotspotReports();
        }
    }
}
