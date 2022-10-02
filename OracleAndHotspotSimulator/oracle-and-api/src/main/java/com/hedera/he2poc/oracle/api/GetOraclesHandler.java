package com.hedera.he2poc.oracle.api;

import com.hedera.he2poc.oracle.HCSOracle;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GetOraclesHandler implements Handler<RoutingContext> {

    private final HCSOracle hcsOracle;
    public GetOraclesHandler(HCSOracle hcsOracle) {
        this.hcsOracle = hcsOracle;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            JsonObject response = new JsonObject();
            if (hcsOracle != null) {
                response = hcsOracle.getDetails();
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
