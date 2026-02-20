package demo.resource;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final Counter createdCounter;
    private final Counter failedCounter;
    private final Timer processingTimer;
    private final List<Map<String, Object>> orders = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    @Inject
    public OrderResource(MeterRegistry registry) {
        this.createdCounter = Counter.builder("orders.created")
                .description("Number of orders successfully created")
                .register(registry);
        this.failedCounter = Counter.builder("orders.failed")
                .description("Number of orders that failed")
                .register(registry);
        this.processingTimer = Timer.builder("orders.processing.time")
                .description("Order processing latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrder(Map<String, Object> body) {
        return processingTimer.record(() -> {
            // Simulate processing latency 10–200 ms
            try {
                Thread.sleep(10 + random.nextInt(191));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Simulate ~10% failure rate
            if (random.nextDouble() < 0.1) {
                failedCounter.increment();
                return Response.serverError()
                        .entity(Map.of("error", "Order processing failed"))
                        .build();
            }

            int id = orders.size() + 1;
            Map<String, Object> order = Map.of(
                    "id", id,
                    "status", "created",
                    "data", body != null ? body : Map.of());
            orders.add(order);
            createdCounter.increment();
            return Response.status(Response.Status.CREATED).entity(order).build();
        });
    }

    @GET
    public List<Map<String, Object>> listOrders() {
        return new ArrayList<>(orders);
    }
}
