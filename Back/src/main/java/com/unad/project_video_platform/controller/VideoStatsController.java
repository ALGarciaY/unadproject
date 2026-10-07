package com.unad.project_video_platform.controller;

import com.unad.project_video_platform.dto.ApiResponse;
import com.unad.project_video_platform.dto.ContentViewSummary;
import com.unad.project_video_platform.dto.VideoStatsRequest;
import com.unad.project_video_platform.entity.VideoStats;
import com.unad.project_video_platform.service.impl.IVideoStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video-stats")
public class VideoStatsController {

    @Autowired
    private IVideoStatsService videoStatsService;

    @PostMapping("/record")
    public ResponseEntity<ApiResponse<VideoStats>> recordStats(@RequestBody VideoStatsRequest request) {
        try {
            VideoStats stats = videoStatsService.recordStats(request);
            return ResponseEntity.ok(ApiResponse.ok("Video stats recorded", stats));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<VideoStats>badRequest(e.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<VideoStats>forbidden(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<VideoStats>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<VideoStats>internalError(e.getMessage()));
        }
    }

    @GetMapping("/views-by-content")
    public ResponseEntity<ApiResponse<List<ContentViewSummary>>> getViewsByContent() {
        return ResponseEntity.ok(ApiResponse.ok("Content view totals consulted", videoStatsService.getViewsByContent()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoStats>>> getAllStats() {
        return ResponseEntity.ok(ApiResponse.ok("Video stats consulted", videoStatsService.getAllStats()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<List<VideoStats>>> getStatsByContent(@PathVariable Integer contentId) {
        return ResponseEntity.ok(ApiResponse.ok("Content stats consulted", videoStatsService.getStatsByContent(contentId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VideoStats>>> getStatsByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok("User stats consulted", videoStatsService.getStatsByUser(userId)));
    }
}
