package com.hedera.he2poc.oracle.api;

import com.hedera.he2poc.oracle.HCSOracle;
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
    private final HCSOracle hcsOracle;
    
    public PostOraclesHandler(HCSOracle hcsOracle) {
        this.hcsOracle = hcsOracle;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            if (hcsOracle == null) {
                throw new Exception("Oracle is not running");
            }
            int epochDuration = 0;
            int minBeaconReports = 0;
            int minWitnessReports = 0;

            JsonObject response = new JsonObject();
            response.put("result", "success");

            if (routingContext.queryParams().contains("epochDuration")) {
                epochDuration = Integer.parseInt(routingContext.queryParams().get("epochDuration"));
            }
            if (routingContext.queryParams().contains("minBeaconReports")) {
                minBeaconReports = Integer.parseInt(routingContext.queryParams().get("minBeaconReports"));
            }
            if (routingContext.queryParams().contains("minWitnessReports")) {
                minWitnessReports = Integer.parseInt(routingContext.queryParams().get("minWitnessReports"));
            }

            if ((epochDuration == 0) && (minBeaconReports == 0) && (minWitnessReports == 0)) {
                throw new Exception("Provide at least one of epochDuration, minEpochReports or minWitnessReports");
            }
            if (epochDuration != 0) {
                if (epochDuration < 10) {
                    throw new Exception("Epoch duration too low, minimum is 10s");
                } else {
                    hcsOracle.setEpochDuration(epochDuration);
                }
            }
            if (minBeaconReports != 0) {
                if (minBeaconReports < 1) {
                    throw new Exception("Min beacon reports too low, minimum is 1");
                } else {
                    hcsOracle.setMinBeaconReports(minBeaconReports);
                }
            }

            if (minWitnessReports != 0) {
                if (minWitnessReports < 1) {
                    throw new Exception("Min witness reports too low, minimum is 1");
                } else {
                    hcsOracle.setMinWitnessReports(minWitnessReports);
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
