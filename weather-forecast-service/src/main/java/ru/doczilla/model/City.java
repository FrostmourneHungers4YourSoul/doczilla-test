package ru.doczilla.model;

public record City
        (
                String name,
                Double latitude,
                Double longitude,
                String timeZone
        ) {
}
