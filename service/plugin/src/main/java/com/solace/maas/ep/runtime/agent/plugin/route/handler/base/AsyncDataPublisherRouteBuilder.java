package com.solace.maas.ep.runtime.agent.plugin.route.handler.base;

import com.solace.maas.ep.runtime.agent.plugin.constants.RouteConstants;
import com.solace.maas.ep.runtime.agent.plugin.processor.logging.MDCProcessor;
import com.solace.maas.ep.runtime.agent.plugin.route.manager.RouteManager;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

public class AsyncDataPublisherRouteBuilder extends DataPublisherRouteBuilder {
    private final AsyncRoutePublisherImpl asyncRoutePublisher;

    /**
     * @param processor    The Processor handling the Data Collection for a Scan.
     * @param routeId      The Unique Identifier for this Route.
     * @param routeType
     * @param routeManager The list of Route Destinations the Data Collection events will be streamed to.
     * @param mdcProcessor The Processor handling the MDC data for logging.
     */
    public AsyncDataPublisherRouteBuilder(Processor processor, AsyncRoutePublisherImpl asyncRoutePublisher, String routeId,
                                          String routeType, RouteManager routeManager, MDCProcessor mdcProcessor) {
        super(processor, routeId, routeType, routeManager, mdcProcessor);

        this.asyncRoutePublisher = asyncRoutePublisher;
    }

    @Override
    public void configure() {
        interceptFrom()
                .setHeader(RouteConstants.SCAN_TYPE, constant(routeType))
                .process(mdcProcessor);

        CamelReactiveStreamsService camel = CamelReactiveStreams.get(getContext());

        from("seda:asyncSubscriber?blockWhenFull=true&size=1000000")
                .routeId(routeId)
                .to("reactive-streams:asyncProcessing");

        from("reactive-streams:asyncEvent")
                .process(processor)
                .log("${body}");

        Publisher<Exchange> exchanges = camel.fromStream("asyncProcessing");

        Flux.from(exchanges)
                .publishOn(Schedulers.boundedElastic())
                .subscribe(asyncRoutePublisher::start);

        if (Objects.nonNull(routeManager)) {
            routeManager.setupRoute(routeId);
        }
    }
}
