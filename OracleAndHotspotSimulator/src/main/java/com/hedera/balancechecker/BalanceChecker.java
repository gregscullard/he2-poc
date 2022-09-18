package com.hedera.balancechecker;

import com.hedera.hashgraph.sdk.*;
import com.hedera.yamlconfig.YamlConfigManager;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Log4j2
public class BalanceChecker implements Runnable {
    // thread management
    private boolean doStop = false;
    private int intervalMs = 10000;
    // Hedera client
    private PrivateKey privateKey;
    private AccountId accountId;
    private String network;
    private List<AccountId> accountIds;
    private final Client client;
    private final Hbar hotspotThresholdAndTopup;
    private final Hbar treasuryThresholdAndTopup = new Hbar(10);
    private final YamlConfigManager yamlConfigManager;

    public BalanceChecker(List<AccountId> accountIds, PrivateKey privateKey, AccountId accountId, String network) throws FileNotFoundException {
        this.privateKey = privateKey;
        this.accountId = accountId;
        this.network = network;
        this.accountIds = accountIds;

        this.client = Client.forName(network);
        this.client.setOperator(accountId, privateKey);
        this.client.setMaxNodeAttempts(1);
        this.client.setMaxAttempts(1);

        this.yamlConfigManager = new YamlConfigManager();
        this.hotspotThresholdAndTopup  = new Hbar(yamlConfigManager.getInitialHotspotBalance());
    }

    public synchronized void stop() {
        this.doStop = true;
    }

    public synchronized void setIntervalMs(int intervalMs) {
        this.intervalMs = intervalMs;
    }
    private synchronized boolean keepRunning() {
        return this.doStop == false;
    }

    public void run() {
        log.info("Balance Checker running");
        while(keepRunning()) {
            for (int i=0; i < accountIds.size(); i++) {
                checkBalance(accountIds.get(i), hotspotThresholdAndTopup);
                if (! keepRunning()) {
                    break;
                }
                checkBalance(AccountId.fromString(yamlConfigManager.getTreasuryAccount()), treasuryThresholdAndTopup);
                if (! keepRunning()) {
                    break;
                }
            }
        }
    }
    private void checkBalance(AccountId accountId, Hbar amount) {
        try {
            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(client);
            if (balance.hbars.compareTo(amount) < 0) {
                // below 5, add 5
                new TransferTransaction()
                        .addHbarTransfer(client.getOperatorAccountId(), amount.negated())
                        .addHbarTransfer(accountId, amount)
                        .execute(client);
            }
        } catch (TimeoutException e) {
            log.error(e);
        } catch (PrecheckStatusException e) {
            log.error(e);
        }
    }
}

