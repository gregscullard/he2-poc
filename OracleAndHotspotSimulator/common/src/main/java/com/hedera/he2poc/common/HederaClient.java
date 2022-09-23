package com.hedera.he2poc.common;

import com.hedera.hashgraph.sdk.Client;

public class HederaClient {
    public static Client clientFromEnv() {
        Secrets secrets = new Secrets();
        Client client = Client.forName(secrets.network());
        client.setOperator(secrets.accountId(), secrets.privateKey());
        return client;
    }

}
