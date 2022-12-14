package com.hedera.he2poc.oracle.api;

import com.hedera.he2poc.hotspot.Hotspots;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PutHotspotsEnableHandler implements Handler<RoutingContext> {

    private final Hotspots hotspots;
    public PutHotspotsEnableHandler(Hotspots hotspots) {
        this.hotspots = hotspots;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            String result = "Invalid hotspot id";
            JsonObject response = new JsonObject();
            if (routingContext.pathParams().containsKey("id")) {
                int id = Integer.parseInt(routingContext.pathParam("id"));
                result = hotspots.startHotspot(id);
            }
            if (result.equals("success")) {
                response.put("result", result);
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodeToBuffer(response));
            } else {
                throw new Exception(result);
            }
        } catch (Exception error) {
            log.error(error);
            routingContext.fail(500, error);
        }
    }
}
