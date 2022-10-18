package com.solace.maas.ep.runtime.agent.service;

import com.solace.maas.ep.runtime.agent.plugin.route.handler.base.AsyncWrapper;
import com.solace.maas.ep.runtime.agent.plugin.route.manager.AsyncManager;
import com.solace.maas.ep.runtime.agent.repository.model.scan.AsyncScanProcessEntity;
import com.solace.maas.ep.runtime.agent.repository.scan.AsyncScanProcessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AsyncStorageService implements AsyncManager {
    private final AsyncScanProcessRepository repository;

    private final Map<String, AsyncWrapper> asyncWrapperMap;

    @Autowired
    public AsyncStorageService(AsyncScanProcessRepository repository) {
        this.repository = repository;
        this.asyncWrapperMap = new ConcurrentHashMap<>();
    }

    @Override
    public void storeAsync(AsyncWrapper wrapper, String scanId, String scanType) {
        asyncWrapperMap.put(scanId + "_" + scanType, wrapper);

        repository.save(AsyncScanProcessEntity.builder()
                .id(UUID.randomUUID().toString())
                .active(true)
                .scanId(scanId)
                .scanType(scanType)
                .build());
    }

    @Override
    public void stopAsync(String scanId, String scanType) {
        AsyncWrapper asyncWrapper = asyncWrapperMap.remove(scanId + "_" + scanType);

        if (Objects.nonNull(asyncWrapper)) {
            asyncWrapper.terminate();

            repository.findAsyncScanProcessEntityByScanIdAndScanType(scanId, scanType)
                    .ifPresent(asyncScanProcessEntity -> {
                        asyncScanProcessEntity.setActive(false);

                        repository.save(asyncScanProcessEntity);
                    });
        }
    }
}
