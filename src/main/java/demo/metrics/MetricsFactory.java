package demo.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class MetricsFactory {

    public static MeterRegistry create() {
        OtlpConfig config = new OtlpConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String url() {
                String endpoint = System.getenv("GRAFANA_CLOUD_OTLP_ENDPOINT");
                if (endpoint != null && !endpoint.endsWith("/v1/metrics")) {
                    endpoint = endpoint.replaceAll("/+$", "") + "/v1/metrics";
                }
                return endpoint;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(15);
            }

            @Override
            public Map<String, String> headers() {
                String creds = System.getenv("GRAFANA_CLOUD_INSTANCE_ID")
                        + ":"
                        + System.getenv("GRAFANA_CLOUD_API_TOKEN");
                return Map.of("Authorization",
                        "Basic " + Base64.getEncoder().encodeToString(creds.getBytes()));
            }

            @Override
            public Map<String, String> resourceAttributes() {
                return Map.of("service.name", "jersey-metrics-demo");
            }

            // Grafana Cloud Mimir requires CUMULATIVE temporality.
            // Override explicitly so OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE
            // in the environment cannot silently change this.
            @Override
            public AggregationTemporality aggregationTemporality() {
                return AggregationTemporality.CUMULATIVE;
            }
        };

        OtlpMeterRegistry registry = new OtlpMeterRegistry(config, io.micrometer.core.instrument.Clock.SYSTEM);

        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ClassLoaderMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);

        return registry;
    }
}
