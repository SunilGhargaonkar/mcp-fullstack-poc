package com.example.mcp.server.tools;

import org.json.JSONObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class WeatherTool {
    private final WebClient webClient;

    @Value("${openweather.api.url}")
    private String weatherApiUrl;

    @Value("${openweather.api.key}")
    private String weatherApiKey;

    public WeatherTool(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Tool(name = "getWeather", description = "Gets current weather for a given city")
    public Flux<String> getWeather(String city) {
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            return Flux.just("No OpenWeather API key configured.");
        }

        try {
            final String sanitizedCity = city.replaceAll("[^a-zA-Z\\s]", "").trim();
            final String encodedCity = URLEncoder.encode(sanitizedCity, StandardCharsets.UTF_8);
            final URI uri = new URI(weatherApiUrl + "?q=" + encodedCity + "&appid=" + weatherApiKey + "&units=metric");

            return webClient.get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class)
                            .flatMapMany(resp -> {
                                JSONObject json = new JSONObject(resp);
                                if (json.has("main")) {
                                    String message = String.format(
                                            "The current temperature in %s is %.1f °C. Further forecast: %s",
                                            sanitizedCity,
                                            json.getJSONObject("main").getDouble("temp"),
                                            json.getJSONArray("weather").getJSONObject(0).getString("description")
                                    );

                                    return Flux.fromArray(message.split(" "))
                                               .map(word -> word + " ")
                                               .delayElements(java.time.Duration.ofMillis(80));
                                } else {
                                    return Flux.just("Could not fetch the weather for " + sanitizedCity);
                                }
                            })
                            .onErrorResume(ex -> Flux.just("Weather error: " + ex.getMessage()));

        } catch (Exception ex) {
            return Flux.just("Weather error: " + ex.getMessage());
        }
    }
}
