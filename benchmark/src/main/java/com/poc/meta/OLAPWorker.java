package com.poc.meta;

import java.util.List;

public class OLAPWorker implements Runnable {

    private final ProxyClient proxy;
    private final List<String> olapQueries;

    public OLAPWorker(ProxyClient proxy, List<String> olapQueries) {
        this.proxy = proxy;
        this.olapQueries = olapQueries;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                for (String sql : olapQueries) {
                    proxy.executeSelect(sql);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
