package com.hedera;

import com.hedera.api.ApiVerticle;
import com.hedera.balancechecker.BalanceChecker;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hotspot.Hotspots;
import com.hedera.oracle.HCSOracle;
import com.hedera.yamlconfig.YamlConfigManager;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

@Log4j2
public class App {
    private final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) throws Exception {
        App app = new App();
        app.start();
    }

    public void start() throws Exception {

        YamlConfigManager yamlConfigManager = new YamlConfigManager();
        Secrets secrets = new Secrets();

        if (yamlConfigManager.getHotspots().isEmpty()) {
            log.error("No hotspot details found in config.yaml");
            return;
        }
        if (yamlConfigManager.getTopicId().isEmpty()) {
            log.error("No topic configuration found in config.yaml");
            return;
        }

        startApi(yamlConfigManager);

        if (yamlConfigManager.isHotspotsSimulator()) {
            startBalanceChecker(yamlConfigManager, secrets);
            startHotspots(yamlConfigManager);
        }

        if (yamlConfigManager.isOracle()) {
//            MirrorOracle oracle = new MirrorOracle(treasuryAccountKey, treasuryAccount, secrets.network(), topicId);
            Threads.hcsOracle = new HCSOracle(yamlConfigManager, secrets.network());
            Thread oracleThread = new Thread(Threads.hcsOracle);
            oracleThread.start();
        }

//        // stop threads
//        hotspots.stopAll();
//
//        vertx.close();
    }

    private void startHotspots(YamlConfigManager yamlConfigManager) throws FileNotFoundException, InterruptedException {
        int hotspotsToStart = yamlConfigManager.getHotSpotsToStart();
        Hotspots hotspots = new Hotspots();
        for (int i=0; i < hotspotsToStart; i++) {
            hotspots.startHotspot(i);
            // random delay between 1 and 3 seconds
            Random rand = new Random();
            int upperbound = 3;
            int delay = rand.nextInt(upperbound) + 1;
            Thread.sleep(delay * 1000);
        }
    }
    private void startBalanceChecker(YamlConfigManager yamlConfigManager, Secrets secrets) {
        Threads.balanceChecker = new BalanceChecker(yamlConfigManager.getHotspotAccountIds(), secrets.privateKey(), secrets.accountId(), secrets.network());
        Thread balanceCheckerThread = new Thread(Threads.balanceChecker);
        balanceCheckerThread.start();
    }
    private void startApi(YamlConfigManager yamlConfigManager) throws Exception {
        log.info("Starting REST api");
        JsonObject config = new JsonObject();

        String keyOrPass = yamlConfigManager.getHttpsKeyOrPass();
        String certificate = yamlConfigManager.getHttpsCertificate();

        if ( ! keyOrPass.isEmpty() || ! certificate.isEmpty()) {
            if (certificate.isEmpty()) {
                String error = "HTTPS_KEY_OR_PASS provided without HTTPS_CERTIFICATE";
                log.error(error);
                throw new Exception(error);
            }

            if (keyOrPass.isEmpty()) {
                String error = "HTTPS_CERTIFICATE provided without HTTPS_KEY_OR_PASS";
                log.error(error);
                throw new Exception(error);
            }

            if ( ! certificate.endsWith(".jks") && ! certificate.endsWith(".pfx") && ! certificate.endsWith(".pem") && ! certificate.endsWith(".p12")) {
                String error = "HTTPS_KEY_OR_PASS should be a .jks, .pfx, .p12 or .pem file";
                log.error(error);
                throw new Exception(error);
            }

            if ( ! Files.exists(Path.of(certificate))) {
                String error = "HTTPS_CERTIFICATE file cannot be found";
                log.error(error);
                throw new Exception(error);
            }

            if (certificate.endsWith(".pem")) {
                if ( ! Files.exists(Path.of(keyOrPass))) {
                    String error = "HTTPS_KEY_OR_PASS file cannot be found";
                    log.error(error);
                    throw new Exception(error);
                }
            }

            log.info("setting up api servers to use https");
            config.put("server-key-pass", keyOrPass);
            config.put("server-certificate", certificate);
        } else {
            log.info("setting up api servers to use http");
            config.put("server-key-pass", "");
            config.put("server-certificate", "");
        }

        log.info("starting client REST api");
        DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(yamlConfigManager.getApiVerticleCount());
        vertx
                .deployVerticle(ApiVerticle.class.getName(), options)
                .onFailure(error -> {
                    log.error(error.getMessage());
                });
    }
}