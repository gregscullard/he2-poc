package com.hedera.balancechecker;

import com.hedera.hashgraph.sdk.*;
import lombok.extern.log4j.Log4j2;

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

    public BalanceChecker(List<AccountId> accountIds, PrivateKey privateKey, AccountId accountId, String network) {
        this.privateKey = privateKey;
        this.accountId = accountId;
        this.network = network;
        this.accountIds = accountIds;
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
        Hbar thresholdAndTopup = new Hbar(5);
        log.info("Balance Checker running");
        Client client = Client.forName(network);
        client.setOperator(accountId, privateKey);
        client.setMaxNodeAttempts(1);
        client.setMaxAttempts(1);
        while(keepRunning()) {
            for (int i=0; i < accountIds.size(); i++) {
                try {
                    AccountBalance balance = new AccountBalanceQuery()
                            .setAccountId(accountIds.get(i))
                            .execute(client);
                    if (! keepRunning()) {
                        break;
                    }
                    if (balance.hbars.compareTo(thresholdAndTopup) < 0) {
                        // below 5, add 5
                        new TransferTransaction()
                                .addHbarTransfer(client.getOperatorAccountId(), thresholdAndTopup.negated())
                                .addHbarTransfer(accountIds.get(i), thresholdAndTopup)
                                .execute(client);
                    }
                    if (! keepRunning()) {
                        break;
                    }
                } catch (TimeoutException e) {
                    log.error(e);
                } catch (PrecheckStatusException e) {
                    log.error(e);
                }
            }
        }
    }
}

