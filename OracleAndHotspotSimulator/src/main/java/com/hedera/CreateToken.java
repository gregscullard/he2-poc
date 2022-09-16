package com.hedera;

import com.hedera.hashgraph.sdk.*;
import com.hedera.yamlconfig.YamlConfigManager;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateToken {
    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        CreateToken createToken = new CreateToken();
        String tokenName = "HH";
        String tokenSymbol = "HH";
        if (args.length != 0) {
            tokenName = args[0];
            tokenSymbol = args[1];
        }
        createToken.create(tokenName, tokenSymbol);
        System.exit(0);
    }
    public void create(String tokenName, String tokenSymbol) throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
        Client client = Utils.clientFromEnv();

        YamlConfigManager yamlConfigManager = new YamlConfigManager();
        if (yamlConfigManager.getTreasuryAccount().isEmpty()) {
            log.error("Missing treasury account details in config.yaml");
            return;
        }
        if (yamlConfigManager.getTreasuryAccountKey().isEmpty()) {
            log.error("Missing treasury account details in config.yaml");
            return;
        }

        PrivateKey treasuryAccountKey = PrivateKey.fromString(yamlConfigManager.getTreasuryAccountKey());
        AccountId treasuryAccountId = AccountId.fromString(yamlConfigManager.getTreasuryAccount());

        TransactionResponse response = new TokenCreateTransaction()
            .setTokenName(tokenName)
            .setTokenSymbol(tokenSymbol)
            .setDecimals(0)
            .setInitialSupply(100000000)
            .setSupplyType(TokenSupplyType.INFINITE)
            .setSupplyKey(treasuryAccountKey)
            .setTreasuryAccountId(treasuryAccountId)
            .freezeWith(client)
            .sign(treasuryAccountKey)
            .execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        String tokenId = receipt.tokenId.toString();
        log.info("Created Token Id {}", tokenId);

        yamlConfigManager.setTokenId(tokenId);
        yamlConfigManager.save();
        client.close();
    }
}