package com.hedera;

import com.hedera.hashgraph.sdk.*;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateAll {
    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
        CreateAll createAll = new CreateAll();
        createAll.create();
        System.exit(0);
    }
    public void create() throws IOException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
        new CreateToken();
        CreateTopic createTopic = new CreateTopic();
        createTopic.create();
        CreateTreasuryAccount createTreasuryAccount = new CreateTreasuryAccount();
        createTreasuryAccount.create();
        CreateHotspotAccounts createHotspotAccounts = new CreateHotspotAccounts();
        createHotspotAccounts.create();
    }
}