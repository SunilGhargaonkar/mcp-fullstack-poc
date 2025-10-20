package com.example.mcp.server.tools;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import com.example.mcp.server.helper.AuthCodeApp;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CalendarTool {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/London");

    @Value("${google.credentials.path:credentials.json}")
    private String credentialsFilePath;

    private Calendar getService() throws Exception {
        final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        final var clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(new FileInputStream(credentialsFilePath))
        );

        final var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                List.of("https://www.googleapis.com/auth/calendar")
        )
                .setDataStoreFactory(new FileDataStoreFactory(Paths.get("tokens").toFile()))
                .setAccessType("offline")
                .build();

        final Credential credential = new AuthCodeApp(flow).authorize("user");

        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("MCP Calendar")
                .build();
    }

    @Tool(name = "bookEvent", description = "Book a Google Calendar event with title and start datetime (ISO format)")
    public Flux<String> bookEvent(String title, String startIsoUtc, int durationMin) {
        try {
            final Calendar service = getService();

            final Instant instant = Instant.parse(startIsoUtc);
            final ZonedDateTime startZoned = instant.atZone(TIMEZONE);
            final ZonedDateTime endZoned = startZoned.plusMinutes(durationMin);

            final DateTime startDt = new DateTime(Date.from(startZoned.toInstant()));
            final DateTime endDt = new DateTime(Date.from(endZoned.toInstant()));

            final Events existing = service.events().list("primary")
                                           .setTimeMin(startDt)
                                           .setTimeMax(endDt)
                                           .setSingleEvents(true)
                                           .execute();

            if (!existing.getItems().isEmpty()) {
                return Flux.just("Conflict: already booked event '" + existing.getItems().get(0).getSummary() + "'");
            }

            final Event event = new Event()
                    .setSummary(title)
                    .setStart(new EventDateTime().setDateTime(startDt).setTimeZone(TIMEZONE.toString()))
                    .setEnd(new EventDateTime().setDateTime(endDt).setTimeZone(TIMEZONE.toString()))
                    .setDescription("Created via MCP");

            final Event created = service.events().insert("primary", event).execute();

            return Flux.fromArray(("Event booked successfully: " + created.getHtmlLink()).split(" "))
                       .delayElements(java.time.Duration.ofMillis(100));

        } catch (Exception ex) {
            return Flux.just("Calendar error: " + ex.getMessage());
        }
    }
}
