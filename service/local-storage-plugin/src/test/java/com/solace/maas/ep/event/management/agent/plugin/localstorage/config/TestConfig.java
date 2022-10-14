package com.solace.maas.ep.event.management.agent.plugin.localstorage.config;

import com.solace.maas.ep.event.management.agent.plugin.localstorage.config.processor.RouteCompleteProcessorImpl;
import com.solace.maas.ep.event.management.agent.plugin.localstorage.config.processor.ScanCompleteProcessorImpl;
import com.solace.maas.ep.event.management.agent.plugin.processor.logging.MDCProcessor;
import com.solace.maas.ep.event.management.agent.plugin.route.manager.FileStoreManager;
import com.solace.maas.ep.event.management.agent.plugin.processor.logging.RouteCompleteProcessor;
import com.solace.maas.ep.event.management.agent.plugin.processor.logging.ScanCompleteProcessor;
import com.solace.maas.ep.event.management.agent.plugin.processor.output.file.event.AggregatedFileEvent;
import com.solace.maas.ep.event.management.agent.plugin.processor.output.file.event.DataCollectionFileEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class TestConfig {
    @Bean
    public FileStoreManager fileStoreManager() {
        return new FileStoreManager() {
            @Override
            public void storeRecord(DataCollectionFileEvent dataCollectionFileEvent) {

            }

            @Override
            public void storeRecord(AggregatedFileEvent aggregatedFileEvent) {

            }
        };
    }

    @Bean
    public RouteCompleteProcessor routeCompleteProcessor() {
        return new RouteCompleteProcessorImpl();
    }

    @Bean
    public ScanCompleteProcessor scanCompleteProcessor() {
        return new ScanCompleteProcessorImpl();
    }

    @Bean
    public MDCProcessor mdcProcessor() {
        return mock(MDCProcessor.class);
    }

}