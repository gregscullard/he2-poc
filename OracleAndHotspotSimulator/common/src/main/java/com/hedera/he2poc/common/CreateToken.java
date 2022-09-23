package com.hedera.he2poc.common;

import com.hedera.hashgraph.sdk.*;
import com.hedera.he2poc.common.yamlconfig.YamlConfigManager;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateToken {
    private final Client client = HederaClient.clientFromEnv();

    public CreateToken() throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
        YamlConfigManager yamlConfigManager = new YamlConfigManager();

        PrivateKey treasuryAccountKey = PrivateKey.fromString(yamlConfigManager.getTreasuryAccountKey());
        AccountId treasuryAccountId = AccountId.fromString(yamlConfigManager.getTreasuryAccount());

        if (yamlConfigManager.getTreasuryAccount().isEmpty()) {
            log.error("Missing treasury account details in config.yaml");
            return;
        }
        if (yamlConfigManager.getTreasuryAccountKey().isEmpty()) {
            log.error("Missing treasury account details in config.yaml");
            return;
        }

        String tokenId = createFungibleToken("HHFT", "HHFT", treasuryAccountId, treasuryAccountKey);

        String nftTokenId = createNonFungibleToken("HHNFT", "HHNFT", treasuryAccountId, treasuryAccountKey);

        yamlConfigManager.setTokenId(tokenId);
        yamlConfigManager.setNftTokenId(nftTokenId);
        yamlConfigManager.save();
        client.close();

    }

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {

        new CreateToken();
        System.exit(0);
    }
    public String createFungibleToken(String tokenName, String tokenSymbol, AccountId treasuryAccountId, PrivateKey treasuryAccountKey) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {

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
                .execute(this.client);

        TransactionReceipt receipt = response.getReceipt(client);
        String tokenId = receipt.tokenId.toString();
        log.info("Created Token Id {}", tokenId);

        return tokenId;
    }
    public String createNonFungibleToken(String tokenName, String tokenSymbol, AccountId treasuryAccountId, PrivateKey treasuryAccountKey) throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {

        TransactionResponse response = new TokenCreateTransaction()
                .setTokenName(tokenName)
                .setTokenSymbol(tokenSymbol)
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setSupplyType(TokenSupplyType.INFINITE)
                .setSupplyKey(treasuryAccountKey)
                .setTreasuryAccountId(treasuryAccountId)
                .freezeWith(client)
                .sign(treasuryAccountKey)
                .execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        String tokenId = receipt.tokenId.toString();
        log.info("Created NFT Token Id {}", tokenId);
        return tokenId;
    }
}