package com.smartcampus.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsRepository extends JpaRepository<AnalyticsReport, Long> {
}