package demo.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Path("/simulate")
@Produces(MediaType.APPLICATION_JSON)
public class SimulatorResource {

    private final OrderResource orderResource;

    @Inject
    public SimulatorResource(OrderResource orderResource) {
        this.orderResource = orderResource;
    }

    @POST
    @Path("/burst")
    public Response burst(@QueryParam("count") int count) {
        if (count <= 0 || count > 10_000) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "count must be between 1 and 10000"))
                    .build();
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(count, 20));
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final int orderId = i + 1;
            futures.add(pool.submit(() ->
                    orderResource.createOrder(Map.of("simulatedOrder", orderId))));
        }

        pool.shutdown();

        int submitted = futures.size();
        return Response.accepted(Map.of(
                "message", "Burst submitted",
                "submitted", submitted)).build();
    }
}
