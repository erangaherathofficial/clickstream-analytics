package com.erangaherath.clickstream.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "page_view_metrics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"page_url", "event_type", "time_window"})
})
public class PageViewMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_url")
    private String pageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;

    @Column(name = "event_count")
    private long eventCount;

    @Column(name = "time_window")
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
}