package com.solace.maas.ep.runtime.agent.plugin.route.manager;

import com.solace.maas.ep.runtime.agent.plugin.route.handler.base.AsyncWrapper;

public interface AsyncManager {
    void storeAsync(AsyncWrapper wrapper, String scanId, String scanType);

    void stopAsync(String scanId, String scanType);
}
