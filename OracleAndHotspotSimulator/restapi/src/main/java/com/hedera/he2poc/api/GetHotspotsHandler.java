package com.hedera.he2poc.api;

import com.hedera.he2poc.hotspot.Hotspots;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GetHotspotsHandler implements Handler<RoutingContext> {

    private final Hotspots hotspots;
    public GetHotspotsHandler(Hotspots hotspots) {
        this.hotspots = hotspots;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            JsonObject response = hotspots.getDetails();
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception error) {
            log.error(error);
            routingContext.fail(500, error);
        }
    }
}
