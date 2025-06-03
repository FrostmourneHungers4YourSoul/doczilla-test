package ru.doczilla.model;

import java.time.LocalDateTime;

public record CachedWeather
        (
                Weather weather,
                LocalDateTime time
        ) {
    public CachedWeather(Weather weather) {
        this(weather, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "cachedTime: " + time + ", " + weather + ']';
    }
}
