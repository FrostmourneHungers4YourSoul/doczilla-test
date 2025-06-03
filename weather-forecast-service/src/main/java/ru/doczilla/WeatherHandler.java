package ru.doczilla;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.doczilla.model.CachedWeather;
import ru.doczilla.model.City;
import ru.doczilla.model.Hourly;
import ru.doczilla.model.Message;
import ru.doczilla.model.Weather;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(WeatherHandler.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search?name={city}&count=1&language=en&format=json";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&hourly=temperature_2m";

    private final ConcurrentHashMap<String, CachedWeather> localCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public WeatherHandler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanUpCache, 0, 3, TimeUnit.MINUTES);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String cityName = exchange.getRequestURI()
                .getQuery()
                .replace("city=", "");

        if (cityName.isEmpty()) {
            Message message = new Message("City parameter is missing");
            sendResponse(exchange, 400,
                    objectMapper.writeValueAsBytes(message));
            return;
        }

        try {
            Weather weather = localCache.containsKey(cityName)
                    ? localCache.get(cityName).weather()
                    : getRequest(cityName, exchange);

            String htmlTemplate;
            try (InputStream inputStream =
                         getClass().getResourceAsStream("/web/weather.html")) {
                if (inputStream == null) {
                    throw new IOException("File not found");
                }
                htmlTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            String htmlResponse = replace(weather, htmlTemplate);

            byte[] responseBytes = htmlResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
    }

    private String replace(Weather weather, String htmlTemplate) throws Exception {
        String name = weather.getCity().name();
        String labelsJson = objectMapper.writeValueAsString(weather.getHourly().times());
        String temperaturesJson = objectMapper.writeValueAsString(weather.getHourly().temperatures2m());

        String temperature = weather.getTemperature();
        String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String latitude = weather.getCity().latitude().toString();
        String longitude = weather.getCity().longitude().toString();
        String timeZone = weather.getCity().timeZone();

        return htmlTemplate
                .replace("{{temperature}}", temperature)
                .replace("{{time}}", datetime)
                .replace("{{latitude}}", latitude)
                .replace("{{longitude}}", longitude)
                .replace("{{timeZone}}", timeZone)
                .replace("{{cityName}}", name)
                .replace("{{labels}}", labelsJson)
                .replace("{{temperatures}}", temperaturesJson);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, byte[] responseBytes)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private Weather getRequest(String cityName, HttpExchange exchange) throws Exception {
        JsonNode geoData = getCoordinate(cityName);

        if (geoData == null || geoData.isEmpty()) {
            Message message = new Message("City not found");
            sendResponse(exchange, 404,
                    objectMapper.writeValueAsBytes(message));
            logger.warn("{}: {}", message.error(), cityName);
            throw new RuntimeException(message.error());
        }

        City city = new City(
                geoData.get(0).get("name").asText(),
                geoData.get(0).get("latitude").asDouble(),
                geoData.get(0).get("longitude").asDouble(),
                geoData.get(0).get("timezone").asText()
        );

        JsonNode weatherData = getWeather(city);
        Hourly hourly = extractHourly(weatherData);

        Weather weather = new Weather(city, hourly);

        CachedWeather cachedWeather = new CachedWeather(weather);
        localCache.put(cityName, cachedWeather);
        logger.info("Cached: key: {}, value: {}", cityName, cachedWeather);
        return weather;
    }

    private JsonNode getWeather(City city) throws Exception {
        String url = FORECAST_URL
                .replace("{lat}", city.latitude().toString())
                .replace("{lon}", city.longitude().toString());

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        logger.info("Weather forecast request: {}", request);

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Weather forecast response: {}", response);

        return objectMapper.readTree(response.body());
    }

    private JsonNode getCoordinate(String city) throws Exception {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = GEOCODING_URL.replace("{city}", encodedCity);

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();


        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Geocoding request: {}", request);

        return objectMapper.readTree(response.body()).get("results");
    }

    public static Hourly extractHourly(JsonNode root) {
        JsonNode hourlyNode = root.get("hourly");

        List<String> times = new ArrayList<>();
        List<Double> temperatures2m = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            times.add(hourlyNode.get("time").get(i).asText().replace("T", " "));
            temperatures2m.add(hourlyNode.get("temperature_2m").get(i).asDouble());
        }
        times.replaceAll(time -> time.substring(11));
        return new Hourly(times, temperatures2m);
    }

    private void cleanUpCache() {
        LocalDateTime now = LocalDateTime.now();
        localCache.entrySet()
                .removeIf(
                        entry -> entry
                                .getValue()
                                .time()
                                .plusMinutes(15)
                                .isBefore(now)
                );
        logger.info("Cache size: {}", localCache.size());
    }

    public void shutdown() {
        logger.info("Shutting down scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.info("Scheduler forcefully stopped.");
            } else {
                logger.info("Scheduler stopped gracefully.");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error("Scheduler shutdown interrupted: {}", e.getMessage());
        }
    }
}
