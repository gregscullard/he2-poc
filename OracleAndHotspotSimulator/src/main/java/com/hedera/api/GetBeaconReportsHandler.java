package com.hedera.api;

import com.hedera.Threads;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GetBeaconReportsHandler implements Handler<RoutingContext> {

    public GetBeaconReportsHandler() {
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            JsonObject response = new JsonObject();
            if (Threads.hcsOracle != null) {
                int id = Integer.parseInt(routingContext.pathParam("id"));
                response = JsonObject.mapFrom(Threads.hcsOracle.getBeaconHistory(id));
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
