package com.hedera.he2poc.common;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Secrets {
    private final PrivateKey privateKey;
    private final AccountId accountId;
    private final String network;
    public Secrets() {
        Dotenv dotenv = Dotenv.configure().directory("..").load();
        this.privateKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));
        this.accountId = AccountId.fromString(dotenv.get("OPERATOR_ID"));;
        this.network = dotenv.get("HEDERA_NETWORK");
    }

    public PrivateKey privateKey() {
        return this.privateKey;
    }

    public String network() {
        return this.network;
    }

    public AccountId accountId() {
        return this.accountId;
    }
}
