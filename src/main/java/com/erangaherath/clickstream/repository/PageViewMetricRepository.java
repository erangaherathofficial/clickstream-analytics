package com.erangaherath.clickstream.repository;

import com.erangaherath.clickstream.model.EventType;
import com.erangaherath.clickstream.model.PageViewMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PageViewMetricRepository extends JpaRepository<PageViewMetric, Long> {

    Optional<PageViewMetric> findByPageUrlAndEventTypeAndTimeWindow(
            String pageUrl, EventType eventType, Instant timeWindow);

    List<PageViewMetric> findByPageUrl(String pageUrl);
}
