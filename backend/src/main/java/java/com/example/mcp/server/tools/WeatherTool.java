package java.com.example.mcp.server.tools;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WeatherTool {
    private final RestClient restClient;

    @Value("${openweather.api.url}")
    private String weatherApiUrl;

    @Value("${openweather.api.key}")
    private String weatherApiKey;

    public WeatherTool(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @McpTool(name = "getWeather", description = "Gets current weather for a given city.")
    public String getWeather(@McpToolParam(description = "City name, for example London or Vancouver") String city) {
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            return "No OpenWeather API key configured.";
        }
        if (city == null || city.isBlank()) {
            return "City is required.";
        }

        try {
            final String sanitizedCity = city.replaceAll("[^a-zA-Z\\s]", "").trim();
            if (sanitizedCity.isBlank()) {
                return "City is required.";
            }
            final String encodedCity = URLEncoder.encode(sanitizedCity, StandardCharsets.UTF_8);
            final URI uri = new URI(weatherApiUrl + "?q=" + encodedCity + "&appid=" + weatherApiKey + "&units=metric");

            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return "Could not fetch the weather for " + sanitizedCity;
            }

            JSONObject json = new JSONObject(response);
            if (!json.has("main")) {
                return "Could not fetch the weather for " + sanitizedCity;
            }

            return String.format(
                    "The current temperature in %s is %.1f C. Conditions: %s.",
                    sanitizedCity,
                    json.getJSONObject("main").getDouble("temp"),
                    json.getJSONArray("weather").getJSONObject(0).getString("description")
            );

        } catch (Exception ex) {
            return "Weather error: " + ex.getMessage();
        }
    }
}
