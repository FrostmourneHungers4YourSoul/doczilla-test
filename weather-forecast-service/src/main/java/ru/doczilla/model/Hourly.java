package ru.doczilla.model;

import java.util.List;

public record Hourly
        (
                List<String> times,
                List<Double> temperatures2m
        ) {
}
