package com.hedera.oracle;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.*;
import com.hedera.proto.Report;
import com.hedera.yamlconfig.YamlConfigManager;
import com.hedera.yamlconfig.YamlHotspot;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
    private final Map<Integer, List<AccountId>> hotspotPaidAccountsById;
    private List<YamlHotspot> hotspots;

    // Hedera client
    public HCSOracle(YamlConfigManager yamlConfigManager, String network) throws Exception {
        this.privateKey = PrivateKey.fromString(yamlConfigManager.getTreasuryAccountKey());
        this.accountId = AccountId.fromString(yamlConfigManager.getTreasuryAccount());
        this.network = network;
        this.topicId = TopicId.fromString(yamlConfigManager.getTopicId());
        this.tokenId = TokenId.fromString(yamlConfigManager.getTokenId());
        this.hotspotPaidAccountsById = yamlConfigManager.getHotspotPaidAccountsByIdMap();
        this.hotspots = yamlConfigManager.getHotspots();

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

            Instant reportTimestamp = message.consensusTimestamp;
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
                    int beaconCount = countersToCheck.getBeaconCount();
                    int witnessCount = countersToCheck.getWitnessCount();
                    if ((beaconCount >= minEpochReports) && (witnessCount >= minWitnessReports)) {
                        log.info("*** Epoch ({}) Rewarding hotspot {}({}) for {} reports and {} witnesses", currentEpoch, hotspots.get(reports.getKey()).getName(), reports.getKey(), beaconCount, witnessCount);
                        int rewardCount = 0;
                        try {
                            long payment = 2;
                            rewardCount = issueBeaconReward(reports.getKey(), payment);
                            // update the counters to show payment
                            countersToCheck.setRewardPaid(payment);
                            reportsToCheck.updateReportsCounter(currentEpoch, countersToCheck);
                            hotspotReportsMap.put(reports.getKey(), reportsToCheck);
                        } catch (PrecheckStatusException e) {
                            log.error(e);
                        } catch (TimeoutException e) {
                            log.error(e);
                        }
                        log.info("*** Rewarded hotspot {}({}) via {} accounts", hotspots.get(reports.getKey()).getName(), reports.getKey(), rewardCount);
                    } else if (beaconCount >= minEpochReports) {
                        log.info("!!! Epoch ({}) Hotspot {}({}) did not beacon sufficiently ({} of {})", currentEpoch, hotspots.get(reports.getKey()).getName(), reports.getKey(), beaconCount, minEpochReports);
                    } else {
                        log.info("!!! Epoch ({}) Hotspot {}({}) was not witnessed sufficiently ({} of {})", currentEpoch, hotspots.get(reports.getKey()).getName(), reports.getKey(), witnessCount, minWitnessReports);
                    }
                }
                currentEpoch = reportEpoch;
                currentEpochStart = timestampSeconds;
            }

            int hotspotId = 0;
            HotspotReports hotspotReports = new HotspotReports();
            // is it a beacon report ?
            if (report.hasBeaconReport()) {
                // beacon report
                hotspotId = report.getBeaconReport().getId();
                // check if there is a report already for this hotspot
                if (hotspotReportsMap.containsKey(hotspotId)) {
                    hotspotReports = hotspotReportsMap.get(hotspotId);
                }

                log.debug("beacon report {}", reportTimestamp.getEpochSecond());

                HotspotReportsCounter hotspotReportsCounter = hotspotReports.getReportByEpoch(reportEpoch, currentEpochStart);
                hotspotReportsCounter.addBeaconCount();
                hotspotReports.updateReportsCounter(reportEpoch, hotspotReportsCounter);
                hotspotReportsMap.put(hotspotId, hotspotReports);
            } else if (report.hasWitnessReport()) {
                // witness report
                log.debug("witness report {}", reportTimestamp.getEpochSecond());
                // beacon report
                hotspotId = report.getWitnessReport().getWitnessedId();
                // check if there is a report already for this hotspot
                if (hotspotReportsMap.containsKey(hotspotId)) {
                    hotspotReports = hotspotReportsMap.get(hotspotId);
                }

                log.debug("beacon report {}", reportTimestamp.getEpochSecond());

                HotspotReportsCounter hotspotReportsCounter = hotspotReports.getReportByEpoch(reportEpoch, currentEpochStart);
                hotspotReportsCounter.addWitnessCount();
                hotspotReports.updateReportsCounter(reportEpoch, hotspotReportsCounter);
                hotspotReportsMap.put(hotspotId, hotspotReports);
            }
        } catch (InvalidProtocolBufferException e) {
            log.error(e);
        }
    }

    //TODO: Run as a thread ?
    private int issueBeaconReward(int hotspotId, long quantity) throws PrecheckStatusException, TimeoutException {
        List<AccountId> paidAccounts = hotspotPaidAccountsById.get(hotspotId);

        if (paidAccounts.size() == 1) {
            new TransferTransaction()
                    .addTokenTransfer(this.tokenId, accountId, -quantity)
                    .addTokenTransfer(this.tokenId, paidAccounts.get(0), quantity)
                    .execute(client);
            return 1;
        } else {
            //TODO make this not assume 2 (and consider rounding)
            long individualQuantity = quantity / paidAccounts.size();
            TransferTransaction transaction = new TransferTransaction()
                    .addTokenTransfer(this.tokenId, accountId, -quantity);
            //TODO make this not assume 2
            transaction.addTokenTransfer(this.tokenId, paidAccounts.get(0), individualQuantity);
            transaction.addTokenTransfer(this.tokenId, paidAccounts.get(1), individualQuantity);

            transaction.execute(client);
            return 2;
        }
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
