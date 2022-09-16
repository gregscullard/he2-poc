package com.hedera.api;

import com.hedera.Utils;
import com.hedera.hotspot.Hotspots;
import com.hedera.yamlconfig.YamlConfigManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * REST API verticle for the client API
 */
@Log4j2
public class ApiVerticle extends AbstractVerticle {
    Hotspots hotspots = new Hotspots();

    public ApiVerticle() throws FileNotFoundException {
    }

    /**
     * Starts the verticle and sets up the necessary handlers for each available endpoint
     * @param startPromise the Promise to callback when complete
     */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        YamlConfigManager yamlConfigManager = new YamlConfigManager();
        int httpPort = yamlConfigManager.getApiPort();

        HttpServerOptions options = Utils.httpServerOptions(config());
        var server = vertx.createHttpServer(options);
        var router = Router.router(vertx);

        // core
        AuthenticationHandler authenticationHandler = new AuthenticationHandler(yamlConfigManager);
        RootHandler rootHandler = new RootHandler();

        // hotspots
        GetHotspotsHandler getHotSpotsHandler = new GetHotspotsHandler(hotspots);
        PostHotspotsHandler postHotspotsHandler = new PostHotspotsHandler(hotspots);

        // hotspot
        PutHotspotsEnableHandler putHotspotsEnableHandler = new PutHotspotsEnableHandler(hotspots);
        PutHotspotsDisableHandler putHotspotsDisableHandler = new PutHotspotsDisableHandler(hotspots);

        // hotspots interval
        PutHotspotsIntervalHandler putHotspotsIntervalHandler = new PutHotspotsIntervalHandler(hotspots);
        // oracles
        PostOraclesHandler postOraclesHandler = new PostOraclesHandler();
        GetOraclesHandler getOraclesHandler = new GetOraclesHandler();
        // reports
        GetBeaconReportsHandler getBeaconReportsHandler = new GetBeaconReportsHandler();

        Set<HttpMethod> allowedMethods = new LinkedHashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.POST, HttpMethod.GET));
        Set<String> allowedHeaders = new LinkedHashSet<>(Arrays.asList("content-type", "x-api-key"));

        router.route()
                .handler(BodyHandler.create())
                .handler(CorsHandler.create("*")
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders))
                .failureHandler(ApiVerticle::failureHandler);

        router.get("/api/v1/hotspots")
                .handler(getHotSpotsHandler);

        router.get("/api/v1/hotspots/beaconReports/:id")
                .handler(getBeaconReportsHandler);

        router.post("/api/v1/hotspots/interval/:interval")
                .handler(authenticationHandler)
                .handler(putHotspotsIntervalHandler);

        router.post("/api/v1/hotspots/:name/:key")
                .handler(authenticationHandler)
                .handler(postHotspotsHandler);

        router.put("/api/v1/hotspots/:id/interval/:interval")
                .handler(authenticationHandler)
                .handler(putHotspotsIntervalHandler);

        router.put("/api/v1/hotspots/:id/disable")
                .handler(authenticationHandler)
                .handler(putHotspotsDisableHandler);

        router.put("/api/v1/hotspots/:id/enable")
                .handler(authenticationHandler)
                .handler(putHotspotsEnableHandler);

        router.post("/api/v1/oracles")
                .handler(authenticationHandler)
                .handler(postOraclesHandler);

        router.get("/api/v1/oracles")
                .handler(getOraclesHandler);

        router.get("/").handler(rootHandler);

        //minBeacons
        //minWitness
        //epoch
        server
                .requestHandler(router)
                .exceptionHandler(error -> {
                    log.error(error);
                })
                .listen(httpPort, result -> {
                    if (result.succeeded()) {
                        log.info("API Web Server Listening on port: {}", httpPort);
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });
    }

    /**
     * Generic failure handler for REST API calls
     *
     * @param routingContext the RoutingContext for which the failure occurred
     */
    private static void failureHandler(RoutingContext routingContext) {
        var response = routingContext.response();

        // if we got into the failure handler the status code
        // has likely been populated
        if (routingContext.statusCode() > 0) {
            response.setStatusCode(routingContext.statusCode());
        }

        var cause = routingContext.failure();
        if (cause != null) {
            log.error(cause, cause);
            response.setStatusCode(500);
            response.setStatusMessage(cause.getMessage());
        }

        response.end();
    }
}
