package com.project.memoireBackend.controller;

import com.project.memoireBackend.dto.DateRangeDTO;
import com.project.memoireBackend.dto.MetricCreateDTO;
import com.project.memoireBackend.dto.MetricDTO;
import com.project.memoireBackend.dto.MetricSummaryDTO;
import com.project.memoireBackend.model.MetricType;
import com.project.memoireBackend.service.MetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    @Autowired
    private MetricService metricService;

    @GetMapping
    public ResponseEntity<Page<MetricDTO>> getAllMetrics(Pageable pageable) {
        return ResponseEntity.ok(metricService.getMetricsPaged(pageable));
    }

    @GetMapping("/database/{databaseId}")
    public ResponseEntity<Page<MetricDTO>> getMetricsByDatabase(@PathVariable Long databaseId, Pageable pageable) {
        return ResponseEntity.ok(metricService.getMetricsByDatabasePaged(databaseId, pageable));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MetricDTO>> getMetricsByType(@PathVariable MetricType type) {
        return ResponseEntity.ok(metricService.getMetricsByType(type));
    }

    @PostMapping("/database/{databaseId}/type/{type}/date-range")
    public ResponseEntity<List<MetricDTO>> getMetricsByDatabaseAndType(
            @PathVariable Long databaseId,
            @PathVariable MetricType type,
            @RequestBody DateRangeDTO dateRange) {
        return ResponseEntity.ok(metricService.getMetricsByDatabaseAndType(
                databaseId, type, dateRange.getStartDate(), dateRange.getEndDate()));
    }

    @GetMapping("/database/{databaseId}/type/{type}/latest")
    public ResponseEntity<MetricDTO> getLatestMetricByDatabaseAndType(
            @PathVariable Long databaseId,
            @PathVariable MetricType type) {
        MetricDTO metric = metricService.getLatestMetricByDatabaseAndType(databaseId, type);
        if (metric == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(metric);
    }

    @PostMapping("/database/{databaseId}/type/{type}/summary")
    public ResponseEntity<MetricSummaryDTO> getMetricSummary(
            @PathVariable Long databaseId,
            @PathVariable MetricType type,
            @RequestBody DateRangeDTO dateRange) {
        return ResponseEntity.ok(metricService.getMetricSummary(
                databaseId, type, dateRange.getStartDate(), dateRange.getEndDate()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MetricDTO> createMetric(@Validated @RequestBody MetricCreateDTO metricCreateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return ResponseEntity.ok(metricService.createMetric(metricCreateDTO, currentUsername));
    }
}
