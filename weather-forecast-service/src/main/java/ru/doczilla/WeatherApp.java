package ru.doczilla;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WeatherApp {

    private static final Logger logger = LoggerFactory.getLogger(WeatherApp.class);

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            WeatherHandler handler = new WeatherHandler();
            server.createContext("/weather", handler);
            server.setExecutor(null);
            server.start();
            logger.info("Server started on port: 8080");


            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down server...");
                server.stop(1);
                handler.shutdown();
                logger.info("Server stopped.");
            }));
        } catch (IOException e) {
            logger.error("IOException: ", e);
        }
    }
}
