package com.hedera.oracle;

import com.hedera.hashgraph.sdk.*;
import com.hedera.json.JsonConstants;
import com.hedera.yamlconfig.YamlConfigManager;
import com.hedera.yamlconfig.YamlHotspot;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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
    private final AccountId accountId;
    private final TopicId topicId;
    private final Client client;

    private final TokenId tokenId;
    private final TokenId nftTokenId;

    private long epochSeconds;
    private int minEpochReports;
    private int minWitnessReports;
    ConcurrentHashMap<Integer, HotspotReports> hotspotReportsMap = new ConcurrentHashMap<>();
    long currentEpoch = 0;
    long currentEpochStart = 0;
    private final Map<Integer, List<AccountId>> hotspotPaidAccountsById = new HashMap<>();
    private final Map<Integer, YamlHotspot> hotspots = new HashMap<>();

    // Hedera client
    public HCSOracle(YamlConfigManager yamlConfigManager, String network) {
        PrivateKey privateKey = PrivateKey.fromString(yamlConfigManager.getTreasuryAccountKey());
        this.accountId = AccountId.fromString(yamlConfigManager.getTreasuryAccount());
        this.topicId = TopicId.fromString(yamlConfigManager.getTopicId());
        this.tokenId = TokenId.fromString(yamlConfigManager.getTokenId());
        this.nftTokenId = TokenId.fromString(yamlConfigManager.getNftTokenId());
        this.minEpochReports = yamlConfigManager.getOracleMinEpochReports();
        this.epochSeconds = yamlConfigManager.getOracleEpochSeconds();
        this.minWitnessReports = yamlConfigManager.getOracleMinWitnessReports();

        this.client = Client.forName(network);
        this.client.setOperator(accountId, privateKey);
        this.client.setMaxNodeAttempts(1);
        this.client.setMaxAttempts(1);
    }

    public synchronized void stop() {
        this.doStop = true;
    }

    private synchronized boolean keepRunning() {
        return !this.doStop;
    }

    public void run() {
        log.info("HCS Oracle running");

        CompletionHandler completionHandler = new CompletionHandler();

        BiConsumer<Throwable, TopicMessage> errorHandler = (error, message) -> log.error(error);

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
                .subscribe(client, this::messageHandler);
    }

    // handles reports
    // counts the number of reports by each hotspot in an "epoch"
    // if the reports in the epoch are >= minEpochReports, a reward is paid
    // for example, if the epoch is 1 day and minEpochReports is 24, then
    // a reward would be paid if the hotspot sent a report more than 24 times.
    private void messageHandler(TopicMessage message) {
//        log.debug("Got HCS Message");
        try {
            String report = new String(message.contents);
            JsonObject jsonReport = new JsonObject(report);
//            Report report = Report.parseFrom(message.contents);

//            if (report.hasHotspot()) {
            if (jsonReport.containsKey(JsonConstants.ACCOUNT_ID)) {
                YamlHotspot yamlHotspot = new YamlHotspot();
//                Hotspot hotspotProto = report.getHotspot();
//                int id = hotspotProto.getId();
                int id = jsonReport.getInteger(JsonConstants.ID);
                yamlHotspot.setId(id);
//                yamlHotspot.setName(hotspotProto.getName());
//                yamlHotspot.setPaidAccounts(hotspotProto.getAccountIdsList());
//                yamlHotspot.setAccountId(hotspotProto.getAccountId());
                yamlHotspot.setName(jsonReport.getString(JsonConstants.NAME));
                yamlHotspot.setPaidAccounts(jsonReport.getJsonArray(JsonConstants.PAID_ACCOUNT_IDS).getList());
                yamlHotspot.setAccountId(jsonReport.getString(JsonConstants.ACCOUNT_ID));
                yamlHotspot.setNft(jsonReport.getString(JsonConstants.NFT));

                hotspots.put(id, yamlHotspot);
                List<AccountId> accountIds = new ArrayList<>();
//                for (String accountId : hotspotProto.getAccountIdsList()) {
                for (String accountId : yamlHotspot.getPaidAccounts()) {
                    accountIds.add(AccountId.fromString(accountId));
                }
                hotspotPaidAccountsById.put(id, accountIds);

                if (! yamlHotspot.getNft().isEmpty()) {
                    // mint an nft
                    TransactionResponse response = new TokenMintTransaction()
                            .setTokenId(this.nftTokenId)
                            .addMetadata(yamlHotspot.getNft().getBytes(StandardCharsets.UTF_8))
                            .execute(client);
                    TransactionReceipt receipt = response.getReceipt(client);

                    NftId nftId = new NftId(tokenId, receipt.serials.get(0));

                    // transfer to hotspot
                    response = new TransferTransaction()
                            .addNftTransfer(nftId, accountId, AccountId.fromString(yamlHotspot.getAccountId()))
                            .execute(client);
                    response.getReceipt(client);
                }
            } else {
                Instant reportTimestamp = message.consensusTimestamp;
                //TODO: calculate epoch properly
                long timestampSeconds = reportTimestamp.getEpochSecond();
                long reportEpoch = timestampSeconds / epochSeconds;
                if (currentEpoch == 0) {
                    currentEpoch = reportEpoch;
                    currentEpochStart = timestampSeconds;
                }
                // check the current epoch is still the same, if different issue payments
                if (currentEpoch != reportEpoch) {
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
                                long payment = 4;
                                rewardCount = issueBeaconReward(reports.getKey(), payment);
                                // update the counters to show payment
                                countersToCheck.setRewardPaid(payment);
                                reportsToCheck.updateReportsCounter(currentEpoch, countersToCheck);
                                hotspotReportsMap.put(reports.getKey(), reportsToCheck);
                            } catch (PrecheckStatusException | TimeoutException e) {
                                log.error(e);
                            }
                            log.info("*** Rewarded hotspot {}({}) via {} accounts", hotspots.get(reports.getKey()).getName(), reports.getKey(), rewardCount);
                        } else {
                            try {
                                log.info("!!! Epoch ({}) Hotspot {}({}) was not witnessed sufficiently ({} of {})", currentEpoch, hotspots.get(reports.getKey()).getName(), reports.getKey(), witnessCount, minWitnessReports);
                                int rewardCount = 0;
                                try {
                                    long payment = 2;
                                    rewardCount = issueBeaconReward(reports.getKey(), payment);
                                    // update the counters to show payment
                                    countersToCheck.setRewardPaid(payment);
                                    reportsToCheck.updateReportsCounter(currentEpoch, countersToCheck);
                                    hotspotReportsMap.put(reports.getKey(), reportsToCheck);
                                } catch (PrecheckStatusException | TimeoutException e) {
                                    log.error(e);
                                }
                                log.info("*** Rewarded hotspot {}({}) via {} accounts", hotspots.get(reports.getKey()).getName(), reports.getKey(), rewardCount);
                            } catch (Exception e) {
                                log.error(e);
                            }
                        }
                    }
                    currentEpoch = reportEpoch;
                    currentEpochStart = timestampSeconds;
                }

                int hotspotId = jsonReport.getInteger(JsonConstants.ID);
                if (hotspotId != 0) {
                    HotspotReports hotspotReports = new HotspotReports();
                    // is it a beacon report ?
//                    if (report.hasBeaconReport()) {
                    if (jsonReport.containsKey(JsonConstants.WITNESSED_ID)) {
                        // witness report
                        //                    log.debug("witness report {}", reportTimestamp.getEpochSecond());
                        // beacon report
//                        hotspotId = report.getWitnessReport().getWitnessedId();
                        hotspotId = jsonReport.getInteger(JsonConstants.WITNESSED_ID);
                        // check if there is a report already for this hotspot
                        if (hotspotReportsMap.containsKey(hotspotId)) {
                            hotspotReports = hotspotReportsMap.get(hotspotId);
                        }

                        HotspotReportsCounter hotspotReportsCounter = hotspotReports.getReportByEpoch(reportEpoch, currentEpochStart);
                        hotspotReportsCounter.addWitnessCount();
                        hotspotReports.updateReportsCounter(reportEpoch, hotspotReportsCounter);
                        hotspotReportsMap.put(hotspotId, hotspotReports);
                    } else {
                        // beacon report
                        // check if there is a report already for this hotspot
                        if (hotspotReportsMap.containsKey(hotspotId)) {
                            hotspotReports = hotspotReportsMap.get(hotspotId);
                        }

//                    log.debug("beacon report {}", reportTimestamp.getEpochSecond());
                        HotspotReportsCounter hotspotReportsCounter = hotspotReports.getReportByEpoch(reportEpoch, currentEpochStart);
                        hotspotReportsCounter.addBeaconCount();
                        hotspotReports.updateReportsCounter(reportEpoch, hotspotReportsCounter);
                        hotspotReportsMap.put(hotspotId, hotspotReports);
                    }
                }
            }
//        } catch (InvalidProtocolBufferException e) {
//            log.error(e);
        } catch (Exception e) {
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
