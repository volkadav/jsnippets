package com.norrisjackson.jsnippets.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health indicator that reports JVM and system memory statistics.
 * Contributes to the /actuator/health endpoint under "memory".
 */
@Component("memory")
public class MemoryHealthIndicator implements HealthIndicator {

    private static final double BYTES_PER_MB = 1024.0 * 1024.0;
    private static final double WARNING_THRESHOLD = 0.90; // 90% usage triggers warning

    @Override
    public Health health() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Runtime runtime = Runtime.getRuntime();

        // Calculate usage percentages
        double heapUsedPercent = heapUsage.getMax() > 0
            ? (double) heapUsage.getUsed() / heapUsage.getMax()
            : 0;

        // Determine health status based on heap usage
        Health.Builder builder;
        if (heapUsedPercent >= WARNING_THRESHOLD) {
            builder = Health.down().withDetail("warning", "Heap memory usage exceeds 90%");
        } else {
            builder = Health.up();
        }

        // JVM Heap Memory
        Map<String, Object> heap = new LinkedHashMap<>();
        heap.put("used", formatMB(heapUsage.getUsed()));
        heap.put("committed", formatMB(heapUsage.getCommitted()));
        heap.put("max", formatMB(heapUsage.getMax()));
        heap.put("usedPercent", formatPercent(heapUsedPercent));

        // JVM Non-Heap Memory (Metaspace, code cache, etc.)
        Map<String, Object> nonHeap = new LinkedHashMap<>();
        nonHeap.put("used", formatMB(nonHeapUsage.getUsed()));
        nonHeap.put("committed", formatMB(nonHeapUsage.getCommitted()));
        if (nonHeapUsage.getMax() > 0) {
            nonHeap.put("max", formatMB(nonHeapUsage.getMax()));
        }

        // Runtime memory (simplified view)
        Map<String, Object> runtimeMem = new LinkedHashMap<>();
        runtimeMem.put("free", formatMB(runtime.freeMemory()));
        runtimeMem.put("total", formatMB(runtime.totalMemory()));
        runtimeMem.put("max", formatMB(runtime.maxMemory()));

        // System/OS memory (if available)
        Map<String, Object> system = getSystemMemory();

        return builder
                .withDetail("heap", heap)
                .withDetail("nonHeap", nonHeap)
                .withDetail("runtime", runtimeMem)
                .withDetail("system", system)
                .build();
    }

    /**
     * Attempts to get system-level memory info via the OperatingSystemMXBean.
     * This works on most JVMs but the extended attributes are JVM-specific.
     */
    private Map<String, Object> getSystemMemory() {
        Map<String, Object> system = new LinkedHashMap<>();

        try {
            java.lang.management.OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

            if (osMxBean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                long totalPhysical = osBean.getTotalMemorySize();
                long freePhysical = osBean.getFreeMemorySize();
                long usedPhysical = totalPhysical - freePhysical;

                system.put("totalPhysical", formatMB(totalPhysical));
                system.put("freePhysical", formatMB(freePhysical));
                system.put("usedPhysical", formatMB(usedPhysical));

                if (totalPhysical > 0) {
                    system.put("usedPercent", formatPercent((double) usedPhysical / totalPhysical));
                }

                // Swap memory
                long totalSwap = osBean.getTotalSwapSpaceSize();
                long freeSwap = osBean.getFreeSwapSpaceSize();
                if (totalSwap > 0) {
                    system.put("totalSwap", formatMB(totalSwap));
                    system.put("freeSwap", formatMB(freeSwap));
                }
            } else {
                system.put("note", "System memory info not available on this JVM");
            }
        } catch (Exception e) {
            system.put("note", "System memory info not available: " + e.getMessage());
        }

        return system;
    }

    private String formatMB(long bytes) {
        return String.format("%.2f MB", bytes / BYTES_PER_MB);
    }

    private String formatPercent(double ratio) {
        return String.format("%.1f%%", ratio * 100);
    }
}

