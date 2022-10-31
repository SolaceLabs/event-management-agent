package com.solace.maas.ep.event.management.agent.plugin.route.handler.base;

import com.solace.maas.ep.event.management.agent.plugin.constants.RouteConstants;
import com.solace.maas.ep.event.management.agent.plugin.constants.ScanStatus;
import com.solace.maas.ep.event.management.agent.plugin.constants.ScanStatusType;
import com.solace.maas.ep.event.management.agent.plugin.jacoco.ExcludeFromJacocoGeneratedReport;
import com.solace.maas.ep.event.management.agent.plugin.processor.logging.MDCProcessor;
import com.solace.maas.ep.event.management.agent.plugin.route.manager.RouteManager;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.util.Objects;

import static org.apache.camel.support.builder.PredicateBuilder.or;

@ExcludeFromJacocoGeneratedReport
public class DataAggregationRouteBuilder extends DataPublisherRouteBuilder {
    protected final AggregationStrategy aggregationStrategy;

    protected final Integer aggregationSize;

    protected final MDCProcessor mdcProcessor;

    /**
     * @param processor           The Processor handling the Data Collection for a Scan.
     * @param routeId             The Unique Identifier for this Route.
     * @param routeType
     * @param routeManager        The list of Route Destinations the Data Collection events will be streamed to.
     * @param aggregationStrategy
     * @param aggregationSize
     * @param mdcProcessor        The Processor handling the MDC data for logging.
     */
    public DataAggregationRouteBuilder(Processor processor, String routeId, String routeType, RouteManager routeManager,
                                       AggregationStrategy aggregationStrategy, Integer aggregationSize,
                                       MDCProcessor mdcProcessor) {
        super(processor, routeId, routeType, routeManager, mdcProcessor);

        this.aggregationStrategy = aggregationStrategy;
        this.aggregationSize = aggregationSize;
        this.mdcProcessor = mdcProcessor;
    }

    /**
     * Configuring main Camel Route for Messaging Service scans.
     */
    @SuppressWarnings("CPD-START")
    @Override
    public void configure() {
        interceptFrom()
                .setHeader(RouteConstants.SCAN_TYPE, constant(routeType))
                .process(mdcProcessor);

        from("seda:" + routeId + "?blockWhenFull=true&size=1000000")
                // Define a Route ID so we can kill this Route if needed.
                .routeId(routeId)
                .setHeader(RouteConstants.SCAN_TYPE, constant(routeType))
                .setHeader("RECIPIENTS", method(this, "getRecipients(${header."
                        + RouteConstants.SCAN_ID + "})"))
                .setHeader("DESTINATIONS", method(this, "getDestinations(${header."
                        + RouteConstants.SCAN_ID + "})"))
                .setHeader(RouteConstants.SCAN_STATUS, constant(ScanStatus.IN_PROGRESS))
                .setHeader(RouteConstants.SCAN_STATUS_TYPE, constant(ScanStatusType.PER_ROUTE))
                .log("Scan request [${header." + RouteConstants.SCAN_ID + "}]: The status of [${header."
                        + RouteConstants.SCAN_TYPE + "}]" + " is: [" + ScanStatus.IN_PROGRESS + "].")
                .to("seda:scanStatusPublisher")

                .log("Scan request [${header." + RouteConstants.SCAN_ID + "}]: Retrieving [${header." + RouteConstants.SCAN_TYPE
                        + "}] details from messaging service [${header." + RouteConstants.MESSAGING_SERVICE_ID + "}].")

                // Data Collected by the Processor is expected to be an Array. We'll be splitting this Array
                // and streaming it back to interested parties. Interceptors / Destination routes will need to
                // aggregate this data together if they need it all at once.
                .split(body()).shareUnitOfWork().streaming().shareUnitOfWork()
                .choice().when(header(Exchange.SPLIT_COMPLETE).isEqualTo(true))
                .setHeader("DATA_PROCESSING_COMPLETE", constant(true))
                .endChoice()
                .end()
                .log(LoggingLevel.TRACE, "agg element ${body}")
                .aggregate(header(RouteConstants.SCAN_ID))
                .aggregationStrategy(aggregationStrategy)
                .completionSize(aggregationSize)
                .completionPredicate(simple("${header.DATA_PROCESSING_COMPLETE} == true"))
                .process(mdcProcessor)
                // Injecting the Data Collection Processor. This will normally be the processor that
                // connects to the Messaging Service.
                .log(LoggingLevel.TRACE, "agg complete ${body}")
                .process(processor)
                // Data Collected by the Processor is expected to be an Array. We'll be splitting this Array
                // and streaming it back to interested parties. Interceptors / Destination routes will need to
                // aggregate this data together if they need it all at once.
                .split(body()).streaming()
                .shareUnitOfWork()
                .log(LoggingLevel.TRACE, "agg split ${body}")
                // The last aggregation bundle will have "DATA_PROCESSING_COMPLETE" set true in the header for all
                // messages in the aggregation bundle.
                // We want to set the "DATA_PROCESSING_COMPLETE" to false, except for the last message in the
                // aggregation bundle.
                .choice().when(or(header(Exchange.SPLIT_COMPLETE).isEqualTo(false),
                        header("DATA_PROCESSING_COMPLETE").isEqualTo(false)))
                .setHeader("DATA_PROCESSING_COMPLETE", constant(false))
                .endChoice()
                .end()
                // The Route Interceptors are injected here. They are called Asynchronously and don't return a response
                // to this Route.
                .recipientList().header("RECIPIENTS").delimiter(";")
                .shareUnitOfWork()
                // Transforming the Events to JSON. Do we need to do this here? Maybe we should delegate this to the
                // destinations instead?
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.TRACE, "destinations ${body}")
                // The Destinations receiving the Data Collection events get called here.
                .recipientList().header("DESTINATIONS").delimiter(";")
                .shareUnitOfWork()
                .to("seda:processScanStatus");

        if (Objects.nonNull(routeManager)) {
            routeManager.setupRoute(routeId);
        }
    }
}
