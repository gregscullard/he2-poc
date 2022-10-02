package com.hedera.he2poc.oracle.api;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RootHandler implements Handler<RoutingContext>  {

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.response()
            .putHeader("content-type", "application/json")
            .end();
    }
}
