package demo.config;

import demo.metrics.MetricsFactory;
import demo.resource.InventoryResource;
import demo.resource.OrderResource;
import demo.resource.SimulatorResource;
import io.micrometer.core.instrument.MeterRegistry;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.micrometer.server.DefaultJerseyTagsProvider;
import org.glassfish.jersey.micrometer.server.MetricsApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class AppConfig extends ResourceConfig {

    private final MeterRegistry registry;

    public AppConfig() {
        this.registry = MetricsFactory.create();

        register(JacksonFeature.class);

        register(new MetricsApplicationEventListener(
                registry,
                new DefaultJerseyTagsProvider(),
                "http.server.requests",
                true));

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(registry).to(MeterRegistry.class);
            }
        });

        register(OrderResource.class);
        register(InventoryResource.class);
        register(SimulatorResource.class);
    }

    public MeterRegistry getRegistry() {
        return registry;
    }
}
