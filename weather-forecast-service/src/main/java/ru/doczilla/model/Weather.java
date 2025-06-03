package ru.doczilla.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Weather {
    private String name;
    private String temperature;
    private String time;
    private City city;
    private Hourly hourly;

    public Weather() {}

    public Weather(Weather weather) {
        this.name = weather.getName();
        this.temperature = weather.getTemperature();
        this.time = weather.getTime();
        this.city = weather.getCity();
        this.hourly = weather.getHourly();
    }

    public Weather(City city, Hourly hourly) {
        this.city = city;
        this.hourly = hourly;
        name = city.name();
        current();
    }

    private void current() {
        int index = Integer.parseInt(
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("HH")
                ));

        this.time = hourly.times().get(index);
        this.temperature = hourly.temperatures2m().get(index).toString() + " °C";
    }

    public String getName() {
        return name;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getTime() {
        return time;
    }

    public City getCity() {
        return city;
    }

    public Hourly getHourly() {
        return hourly;
    }

    @Override
    public String toString() {
        return "Weather[" + name + '\'' +
               ", temperature: '" + temperature + '\'' + time + '\'' +
               ", timezone: " + city.timeZone();
    }
}
