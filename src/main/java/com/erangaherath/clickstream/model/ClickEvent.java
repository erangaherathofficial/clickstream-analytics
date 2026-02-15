package com.erangaherath.clickstream.model;

import java.time.Instant;

public record ClickEvent(
        String eventId,
        String sessionId,
        String userId,
        EventType eventType,
        Instant timestamp,
        String pageUrl,
        String elementId,
        String ipAddress,
        String device,
        String browser,
        String location
) {
}
