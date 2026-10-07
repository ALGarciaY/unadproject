package com.unad.project_video_platform.controller;

import com.unad.project_video_platform.dto.ApiResponse;
import com.unad.project_video_platform.dto.ForumConversationResponse;
import com.unad.project_video_platform.dto.NotificationSummaryResponse;
import com.unad.project_video_platform.dto.QuestionRequest;
import com.unad.project_video_platform.entity.Question;
import com.unad.project_video_platform.service.impl.IForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Autowired
    private IForumService forumService;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ForumConversationResponse>>> getAllConversations() {
        return ResponseEntity.ok(ApiResponse.ok("Conversations consulted", forumService.getAllConversations()));
    }

    @GetMapping("/content/{contentId}/questions")
    public ResponseEntity<ApiResponse<List<Question>>> getQuestionsByContent(@PathVariable Integer contentId) {
        return ResponseEntity.ok(ApiResponse.ok("Questions consulted", forumService.getQuestionsByContent(contentId)));
    }

    @GetMapping("/notifications/{userId}")
    public ResponseEntity<ApiResponse<NotificationSummaryResponse>> getNotifications(@PathVariable Integer userId) {
        try {
            NotificationSummaryResponse summary = forumService.getNotificationSummary(userId);
            return ResponseEntity.ok(ApiResponse.ok("Notifications consulted", summary));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<NotificationSummaryResponse>forbidden(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<NotificationSummaryResponse>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<NotificationSummaryResponse>internalError(e.getMessage()));
        }
    }

    @PutMapping("/notifications/{userId}/seen")
    public ResponseEntity<ApiResponse<Void>> markNotificationsSeen(@PathVariable Integer userId) {
        try {
            forumService.markNotificationsSeen(userId);
            return ResponseEntity.ok(ApiResponse.ok("Notifications marked as seen", null));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<Void>forbidden(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>internalError(e.getMessage()));
        }
    }

    @PostMapping("/questions")
    public ResponseEntity<ApiResponse<Question>> createQuestion(@RequestBody QuestionRequest request) {
        try {
            Question created = forumService.createQuestion(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("Question created", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Question>badRequest(e.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<Question>forbidden(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Question>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Question>internalError(e.getMessage()));
        }
    }
}
