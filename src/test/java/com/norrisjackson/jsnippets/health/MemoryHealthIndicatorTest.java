package com.norrisjackson.jsnippets.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryHealthIndicatorTest {

    private MemoryHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new MemoryHealthIndicator();
    }

    @Test
    void health_returnsNonNullHealth() {
        Health health = indicator.health();
        assertThat(health).isNotNull();
    }

    @Test
    void health_statusIsUpUnderNormalConditions() {
        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void health_containsHeapDetails() {
        Health health = indicator.health();
        Map<String, Object> details = health.getDetails();

        assertThat(details).containsKey("heap");
        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) details.get("heap");
        assertThat(heap).containsKeys("used", "committed", "max", "usedPercent");
    }

    @Test
    void health_heapValuesAreFormattedAsMB() {
        Health health = indicator.health();

        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) health.getDetails().get("heap");
        assertThat((String) heap.get("used")).endsWith(" MB");
        assertThat((String) heap.get("committed")).endsWith(" MB");
        assertThat((String) heap.get("max")).endsWith(" MB");
    }

    @Test
    void health_heapUsedPercentIsFormattedCorrectly() {
        Health health = indicator.health();

        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) health.getDetails().get("heap");
        String usedPercent = (String) heap.get("usedPercent");
        assertThat(usedPercent).endsWith("%");
        String numPart = usedPercent.replace("%", "").trim();
        double value = Double.parseDouble(numPart);
        assertThat(value).isBetween(0.0, 100.0);
    }

    @Test
    void health_containsNonHeapDetails() {
        Health health = indicator.health();
        Map<String, Object> details = health.getDetails();

        assertThat(details).containsKey("nonHeap");
        @SuppressWarnings("unchecked")
        Map<String, Object> nonHeap = (Map<String, Object>) details.get("nonHeap");
        assertThat(nonHeap).containsKeys("used", "committed");
    }

    @Test
    void health_containsRuntimeDetails() {
        Health health = indicator.health();
        Map<String, Object> details = health.getDetails();

        assertThat(details).containsKey("runtime");
        @SuppressWarnings("unchecked")
        Map<String, Object> runtime = (Map<String, Object>) details.get("runtime");
        assertThat(runtime).containsKeys("free", "total", "max");
        assertThat((String) runtime.get("free")).endsWith(" MB");
        assertThat((String) runtime.get("total")).endsWith(" MB");
        assertThat((String) runtime.get("max")).endsWith(" MB");
    }

    @Test
    void health_containsSystemDetails() {
        Health health = indicator.health();
        Map<String, Object> details = health.getDetails();

        assertThat(details).containsKey("system");
        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) details.get("system");
        assertThat(system).isNotEmpty();
    }

    @Test
    void health_runtimeValuesArePositive() {
        Health health = indicator.health();

        @SuppressWarnings("unchecked")
        Map<String, Object> runtime = (Map<String, Object>) health.getDetails().get("runtime");
        double free = parseMBValue((String) runtime.get("free"));
        double total = parseMBValue((String) runtime.get("total"));
        double max = parseMBValue((String) runtime.get("max"));

        assertThat(free).isPositive();
        assertThat(total).isPositive();
        assertThat(max).isPositive();
        assertThat(total).isGreaterThanOrEqualTo(free);
    }

    @Test
    void health_multipleCallsReturnConsistentStructure() {
        Health h1 = indicator.health();
        Health h2 = indicator.health();

        assertThat(h1.getDetails().keySet()).isEqualTo(h2.getDetails().keySet());
        assertThat(h1.getStatus()).isEqualTo(h2.getStatus());
    }

    private double parseMBValue(String formatted) {
        return Double.parseDouble(formatted.replace(" MB", "").trim());
    }
}

