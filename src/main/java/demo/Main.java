package demo;

import demo.config.AppConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final String BASE_URI = "http://0.0.0.0:8080/";

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        MeterRegistry registry = config.getRegistry();

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.shutdownNow();
            registry.close();
        }));

        log.info("Jersey app started at {}", BASE_URI);
        Thread.currentThread().join();
    }
}
