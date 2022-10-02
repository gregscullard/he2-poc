package com.hedera.he2poc.common;

import com.hedera.hashgraph.sdk.*;
import com.hedera.he2poc.common.yamlconfig.YamlConfigManager;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateTreasuryAccount {
    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        CreateTreasuryAccount createTreasuryAccount = new CreateTreasuryAccount();
        createTreasuryAccount.create();
        System.exit(0);
    }
    public void create() throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        Client client = HederaClient.clientFromEnv();

        PrivateKey treasuryAccountKey = PrivateKey.generateED25519();

        TransactionResponse response = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(10))
                .setKey(treasuryAccountKey.getPublicKey())
                .execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        String accountId = receipt.accountId.toString();
        log.info("Created Treasury Account Id {}", accountId);

        YamlConfigManager yamlConfigManager = new YamlConfigManager(false);
        yamlConfigManager.setTreasuryAccount(accountId);
        yamlConfigManager.setTreasuryAccountKey(treasuryAccountKey.toString());
        yamlConfigManager.save();
        client.close();
    }
}