package com.hedera;

import com.hedera.hashgraph.sdk.*;
import com.hedera.yamlconfig.YamlConfigManager;
import com.hedera.yamlconfig.YamlHotspot;
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
    }
     public void create() throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
        List<YamlHotspot> hotspotList = new ArrayList<>();

        Client client = Utils.clientFromEnv();
        YamlConfigManager yamlConfigManager = new YamlConfigManager();

        if (yamlConfigManager.getTokenId().isEmpty()) {
            log.error("Token details missing from config.yaml");
            return;
        }

        for (int i=0; i < 10; i++) {
            PrivateKey accountKey = PrivateKey.generateED25519();
            TransactionResponse response = new AccountCreateTransaction()
                    .setInitialBalance(new Hbar(10))
                    .setKey(accountKey.getPublicKey())
                    .execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            String accountId = receipt.accountId.toString();
            log.info("Created Hotspot Account Id {}", accountId);

            response = new TokenAssociateTransaction()
                    .setTokenIds(List.of(TokenId.fromString(yamlConfigManager.getTokenId())))
                    .setAccountId(AccountId.fromString(accountId))
                    .freezeWith(client)
                    .sign(accountKey)
                    .execute(client);
            receipt = response.getReceipt(client);
            log.info("Token associated");

            YamlHotspot yamlHotspot = new YamlHotspot(
                    i,
                    "region.".concat(String.valueOf(i)),
                    accountId,
                    accountKey.toString());
            hotspotList.add(yamlHotspot);
        }


        yamlConfigManager.setHotspots(hotspotList);
        yamlConfigManager.save();
    }
}