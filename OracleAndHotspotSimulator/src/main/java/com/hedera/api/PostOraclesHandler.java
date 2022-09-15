package com.hedera.api;

import com.hedera.Threads;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Creates a new topic
 */
@Log4j2
public class PostOraclesHandler implements Handler<RoutingContext> {
    public PostOraclesHandler() {

    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            if (Threads.hcsOracle == null) {
                throw new Exception("Oracle is not running");
            }
            int epochDuration = 0;
            int minReports = 0;
            JsonObject response = new JsonObject();
            response.put("result", "success");

            if (routingContext.queryParams().contains("epochDuration")) {
                epochDuration = Integer.parseInt(routingContext.queryParams().get("epochDuration"));
            }
            if (routingContext.queryParams().contains("minEpochReports")) {
                minReports = Integer.parseInt(routingContext.queryParams().get("minEpochReports"));
            }

            if ((epochDuration == 0) && (minReports == 0)) {
                throw new Exception("Provide at least one of epochDuration or minEpochReports");
            }
            if (epochDuration != 0) {
                if (epochDuration < 10) {
                    throw new Exception("Epoch duration too low, minimum is 10s");
                } else {
                    Threads.hcsOracle.setEpochDuration(epochDuration);
                }
            }
            if (minReports != 0) {
                if (minReports < 1) {
                    throw new Exception("Min reports too low, minimum is 1");
                } else {
                    Threads.hcsOracle.setMinEpochReports(minReports);
                }
            }
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception error) {
            log.error(error);
            routingContext.fail(500, error);
        }
    }
}
