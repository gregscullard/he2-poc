package com.hedera.he2poc.oracle.api;

import com.google.errorprone.annotations.Var;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class Utils {

    private static final WebClientOptions webClientOptions = new WebClientOptions()
            .setUserAgent("Demo/1.0")
            .setKeepAlive(false);
    private static final WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);

    public static HttpServerOptions httpServerOptions(JsonObject config) {
        @Var HttpServerOptions options = new HttpServerOptions();
        String keyOrPass = config.getString("server-key-pass");
        String certificate = config.getString("server-certificate");
        if ((certificate == null) || (keyOrPass == null)) {
            return options;
        }
        if (certificate.endsWith(".jks")) {
            options.setSsl(true);
            options.setKeyStoreOptions(
                    new JksOptions()
                            .setPath(certificate)
                            .setPassword(keyOrPass));
        } else if (certificate.endsWith(".pfx") || certificate.endsWith(".p12")) {
            options.setSsl(true);
            options.setPfxKeyCertOptions(
                    new PfxOptions()
                            .setPath(certificate)
                            .setPassword(keyOrPass));
        } else if (certificate.endsWith(".pem")) {
            options.setSsl(true);
            options.setPemKeyCertOptions(
                    new PemKeyCertOptions()
                            .setKeyPath(keyOrPass)
                            .setCertPath(certificate)
            );
        }
        return options;
    }
    public static Callable<JsonObject> queryMirror(String mirrorURL, String url, Map<String, String> queryParameters) {
        return () -> {
            var webQuery = webClient
                    .get(443, mirrorURL, url);

            String queryString = mirrorURL.concat(url);
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                webQuery.addQueryParam(entry.getKey(), entry.getValue());
                queryString = queryString.concat("&").concat(entry.getKey()).concat("=").concat(entry.getValue());
            }
            queryString = queryString.replace("messages&", "messages?");
            log.debug(queryString);

            @Var var attempts = 0;
            while (attempts < 5) {
                CompletableFuture<JsonObject> future = new CompletableFuture<>();
                webQuery.as(BodyCodec.jsonObject())
                        .send()
                        .onSuccess(response -> {
                            try {
                                future.complete(response.body());
                            } catch (RuntimeException e) {
                                log.error(e);
                                future.complete(null);
                            }
                        })
                        .onFailure(err -> {
                            log.error(err);
                            future.complete(null);
                        });
                JsonObject response = future.get();
                if (response != null) {
                    log.debug("returning mirror response for {}", url);
                    return response;
                } else {
                    attempts += 1;
                    Thread.sleep(2000);
                    log.warn("Empty response from mirror on {}{}, trying again {} of 5", mirrorURL, url, attempts);
                }
            }
            log.warn("No response from mirror on {} after 5 attempts", url);
            return new JsonObject();
        };
    }

}
