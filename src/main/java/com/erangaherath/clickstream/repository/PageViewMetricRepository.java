package com.erangaherath.clickstream.repository;

import com.erangaherath.clickstream.model.PageViewMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface PageViewMetricRepository extends JpaRepository<PageViewMetric, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO page_view_metrics (page_url, event_type, event_count, time_window)
            VALUES (:pageUrl, :eventType, 1, :timeWindow)
            ON CONFLICT (page_url, event_type, time_window)
            DO UPDATE SET event_count = page_view_metrics.event_count + 1
            """, nativeQuery = true)
    void upsertMetric(String pageUrl, String eventType, Instant timeWindow);

    List<PageViewMetric> findByPageUrl(String pageUrl);
}