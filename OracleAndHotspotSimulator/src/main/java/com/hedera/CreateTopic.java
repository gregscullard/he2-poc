package com.hedera;

import com.hedera.hashgraph.sdk.*;
import com.hedera.yamlconfig.YamlConfigManager;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateTopic {
    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        CreateTopic createTopic = new CreateTopic();
        createTopic.create();
    }
    public void create() throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        Client client = Utils.clientFromEnv();

        TransactionResponse response = new TopicCreateTransaction()
                .execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        String topicId = receipt.topicId.toString();
        log.info("Created Topic Id {}", topicId);

        YamlConfigManager yamlConfigManager = new YamlConfigManager();
        yamlConfigManager.setTopicId(topicId);
        yamlConfigManager.save();

    }
}