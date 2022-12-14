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
    private static int numHotspotsToCreate = 10;
    private static int firstHotspotId = 1;

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        CreateHotspotAccounts createDemoHotspotAccounts = new CreateHotspotAccounts();
        createDemoHotspotAccounts.create(args);
        System.exit(0);
    }

    public void create(String[] args) throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
        List<YamlHotspot> hotspotList = new ArrayList<>();
        YamlConfigManager yamlConfigManager = new YamlConfigManager(false);

        if (args.length != 0) {
            hotspotList = yamlConfigManager.getHotspots();
            firstHotspotId = hotspotList.size() + 1;
            numHotspotsToCreate = Integer.parseInt(args[0]);
            log.info("Adding {} hotspot accounts", numHotspotsToCreate);
        } else {
            log.info("Creating {} new hotspot accounts", numHotspotsToCreate);
        }

        Client client = HederaClient.clientFromEnv();

        if (yamlConfigManager.getTokenId().isEmpty()) {
            log.error("Token details missing from config.yaml");
            return;
        }
        long initialBalance = yamlConfigManager.getInitialHotspotBalance();

        for (int i=0; i <= numHotspotsToCreate; i++) {
            List<String> paidAccounts = new ArrayList<>();

            PrivateKey accountKey = PrivateKey.generateED25519();
            String accountId = createAccount(client, accountKey, initialBalance);

            YamlHotspot yamlHotspot = new YamlHotspot(
                    firstHotspotId + i,
                    "hotspot-".concat(String.valueOf(firstHotspotId + i)),
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