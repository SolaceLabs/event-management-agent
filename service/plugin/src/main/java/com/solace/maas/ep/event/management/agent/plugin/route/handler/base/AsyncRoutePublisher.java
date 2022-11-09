package com.solace.maas.ep.event.management.agent.plugin.route.handler.base;

import org.apache.camel.Exchange;

public interface AsyncRoutePublisher {
    void start(Exchange exchange);

    AsyncWrapper run(Exchange exchange);
}
