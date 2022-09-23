package com.hedera.he2poc.common;

import com.hedera.hashgraph.sdk.*;
import com.hedera.he2poc.common.yamlconfig.YamlConfigManager;
import com.hedera.he2poc.common.yamlconfig.YamlHotspot;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateHotspotAccounts {
    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        CreateHotspotAccounts createHotspotAccounts = new CreateHotspotAccounts();
        createHotspotAccounts.create();
        System.exit(0);
    }
     public void create() throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
        List<YamlHotspot> hotspotList = new ArrayList<>();

        Client client = HederaClient.clientFromEnv();
        YamlConfigManager yamlConfigManager = new YamlConfigManager();

        if (yamlConfigManager.getTokenId().isEmpty()) {
            log.error("Token details missing from config.yaml");
            return;
        }
        long initialBalance = yamlConfigManager.getInitialHotspotBalance();

        String[] names = {"London", "Paris", "New York"};
        for (int i=1; i < 4; i++) {
            List<String> paidAccounts = new ArrayList<>();

            PrivateKey accountKey = PrivateKey.generateED25519();
            String accountId = createAccount(client, accountKey, initialBalance);

            if (i == 2) {
                String otherAccount = createAccount(client, accountKey, initialBalance);
                paidAccounts.add(otherAccount);
                paidAccounts.add(accountId);
            }

            YamlHotspot yamlHotspot = new YamlHotspot(
                    i,
                    names[i-1],
                    accountId,
                    accountKey.toString(),
                    paidAccounts,
                    "nftBlack.png");
            hotspotList.add(yamlHotspot);
        }

        yamlConfigManager.setHotspots(hotspotList);
        yamlConfigManager.save();
        client.close();
    }
    private String createAccount(Client client, PrivateKey privateKey, long balance) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        TransactionResponse response = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(balance))
                .setKey(privateKey.getPublicKey())
                .setMaxAutomaticTokenAssociations(2)
                .execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        String accountId = receipt.accountId.toString();
        log.info("Created Hotspot Account Id {}", accountId);

        return accountId;
    }
}