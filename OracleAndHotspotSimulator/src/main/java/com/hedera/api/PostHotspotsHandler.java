package com.hedera.api;

import com.hedera.hotspot.Hotspots;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Creates a new topic
 */
@Log4j2
public class PostHotspotsHandler implements Handler<RoutingContext> {
    private Hotspots hotspots;
    public PostHotspotsHandler(Hotspots hotspots) {
        this.hotspots = hotspots;
    }

    @Override
    public void handle(RoutingContext routingContext) {

        try {
            String name = routingContext.pathParams().get("name");
            String key = routingContext.pathParams().get("key");
            
            String result = hotspots.add(name, key);
            JsonObject response = new JsonObject();
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
