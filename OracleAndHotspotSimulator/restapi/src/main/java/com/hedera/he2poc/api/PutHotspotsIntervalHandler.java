package com.hedera.he2poc.api;

import com.hedera.he2poc.hotspot.Hotspots;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PutHotspotsIntervalHandler implements Handler<RoutingContext> {

    private final Hotspots hotspots;
    public PutHotspotsIntervalHandler(Hotspots hotspots) {
        this.hotspots = hotspots;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            String result;
            JsonObject response = new JsonObject();
            int interval = Integer.parseInt(routingContext.pathParam("interval"));
            if (routingContext.pathParams().containsKey("id")) {
                int id = Integer.parseInt(routingContext.pathParam("id"));
                result = hotspots.setInterval(id, interval);
            } else {
                result = hotspots.setInterval(interval);
            }
            response.put("result", result);
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception error) {
            log.error(error);
            routingContext.fail(500, error);
        }
    }
}
