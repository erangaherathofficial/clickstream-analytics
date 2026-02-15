package com.erangaherath.clickstream.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "page_view_metrics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pageUrl", "eventType", "timeWindow"})
})
public class PageViewMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pageUrl;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private long eventCount;

    private Instant timeWindow;

    protected PageViewMetric() {
    }

    public PageViewMetric(String pageUrl, EventType eventType, Instant timeWindow) {
        this.pageUrl = pageUrl;
        this.eventType = eventType;
        this.eventCount = 1;
        this.timeWindow = timeWindow;
    }

    public Long getId() {
        return id;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public EventType getEventType() {
        return eventType;
    }

    public long getEventCount() {
        return eventCount;
    }

    public Instant getTimeWindow() {
        return timeWindow;
    }

    public void incrementCount() {
        this.eventCount++;
    }
}
