package com.project.memoireBackend.controller;


import com.project.memoireBackend.dto.ActivityLogDTO;
import com.project.memoireBackend.dto.DateRangeDTO;
import com.project.memoireBackend.model.ActionType;
import com.project.memoireBackend.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<Page<ActivityLogDTO>> getAllActivityLogs(Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getAllActivityLogs(pageable));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<Page<ActivityLogDTO>> getActivityLogsByUser(
            @PathVariable String username, Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivityLogsByUser(username, pageable));
    }

    @GetMapping("/database/{databaseId}")
    public ResponseEntity<Page<ActivityLogDTO>> getActivityLogsByDatabase(
            @PathVariable Long databaseId, Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivityLogsByDatabase(databaseId, pageable));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<ActivityLogDTO>> getActivityLogsByAction(
            @PathVariable ActionType action, Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivityLogsByAction(action, pageable));
    }

    @PostMapping("/date-range")
    public ResponseEntity<List<ActivityLogDTO>> getActivityLogsByDateRange(@RequestBody DateRangeDTO dateRange) {
        return ResponseEntity.ok(activityLogService.getActivityLogsByDateRange(
                dateRange.getStartDate(), dateRange.getEndDate()));
    }
}