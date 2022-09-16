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
        System.exit(0);
    }
     public void create() throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
        List<YamlHotspot> hotspotList = new ArrayList<>();

        Client client = Utils.clientFromEnv();
        YamlConfigManager yamlConfigManager = new YamlConfigManager();

        if (yamlConfigManager.getTokenId().isEmpty()) {
            log.error("Token details missing from config.yaml");
            return;
        }

        TokenId tokenId = TokenId.fromString(yamlConfigManager.getTokenId());
        String[] names = {"London", "Paris", "New York"};
        for (int i=0; i < 3; i++) {
            List<String> paidAccounts = new ArrayList<>();

            PrivateKey accountKey = PrivateKey.generateED25519();
            String accountId = createAndAssociate(client, accountKey, tokenId);

            if (i == 1) {
                String otherAccount = createAndAssociate(client, accountKey, tokenId);
                paidAccounts.add(otherAccount);
                otherAccount = createAndAssociate(client, accountKey, tokenId);
                paidAccounts.add(otherAccount);
            }

            YamlHotspot yamlHotspot = new YamlHotspot(
                    i,
                    names[i],
                    accountId,
                    accountKey.toString(),
                    paidAccounts);
            hotspotList.add(yamlHotspot);
        }

        yamlConfigManager.setHotspots(hotspotList);
        yamlConfigManager.save();
        client.close();
    }
    private String createAndAssociate(Client client, PrivateKey privateKey, TokenId tokenId) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        TransactionResponse response = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(10))
                .setKey(privateKey.getPublicKey())
                .execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        String accountId = receipt.accountId.toString();
        log.info("Created Hotspot Account Id {}", accountId);

        response = new TokenAssociateTransaction()
                .setTokenIds(List.of(tokenId))
                .setAccountId(AccountId.fromString(accountId))
                .freezeWith(client)
                .sign(privateKey)
                .execute(client);
        response.getReceipt(client);
        log.info("Token associated");
        return accountId;
    }
}