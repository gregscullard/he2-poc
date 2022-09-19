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
            // yes, passing a private key over a http(s) connection is not a good idea,
            // however for the purpose of this POC, the simulator needs to know the hotspot's private key
            // in order to submit messages.
            // in practice a hotspot would never share its private key
            String key = routingContext.pathParams().get("key");
            String nft = routingContext.pathParams().get("nft");
            
            String result = hotspots.add(name, key, nft);
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
