package demo.resource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private final AtomicInteger stockLevel = new AtomicInteger(100);

    @Inject
    public InventoryResource(MeterRegistry registry) {
        Gauge.builder("inventory.stock.level", stockLevel, AtomicInteger::get)
                .description("Current inventory stock level")
                .register(registry);
    }

    @GET
    public Map<String, Integer> getStock() {
        return Map.of("stockLevel", stockLevel.get());
    }

    @POST
    public Response addStock(@QueryParam("amount") int amount) {
        if (amount <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "amount must be positive"))
                    .build();
        }
        int newLevel = stockLevel.addAndGet(amount);
        return Response.ok(Map.of("stockLevel", newLevel)).build();
    }

    @DELETE
    public Response removeStock(@QueryParam("amount") int amount) {
        if (amount <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "amount must be positive"))
                    .build();
        }
        int newLevel = stockLevel.updateAndGet(current -> Math.max(0, current - amount));
        return Response.ok(Map.of("stockLevel", newLevel)).build();
    }
}
