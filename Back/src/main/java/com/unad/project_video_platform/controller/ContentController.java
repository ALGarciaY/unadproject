package com.unad.project_video_platform.controller;

import com.unad.project_video_platform.dto.ApiResponse;
import com.unad.project_video_platform.dto.VideoStatsRequest;
import com.unad.project_video_platform.entity.Video;
import com.unad.project_video_platform.entity.VideoStats;
import com.unad.project_video_platform.service.LocalContentMaterialStorageService;
import com.unad.project_video_platform.service.impl.IVideoService;
import com.unad.project_video_platform.service.impl.IVideoStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    @Autowired
    private IVideoService videoService;

    @Autowired
    private IVideoStatsService videoStatsService;

    @Autowired
    private LocalContentMaterialStorageService localContentMaterialStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Video>>> getAllContent() {
        List<Video> content = videoService.getAllVideos();
        return ResponseEntity.ok(ApiResponse.ok("Content consulted", content));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Video>> getContentById(@PathVariable Integer id) {
        return videoService.getVideoById(id)
                .map(content -> ResponseEntity.ok(ApiResponse.ok("Content found", content)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<Video>notFound("Content not found with id: " + id)));
    }

    @GetMapping("/materials/{storedFileName:.+}")
    public ResponseEntity<Resource> getMaterial(@PathVariable String storedFileName) {
        Resource resource = localContentMaterialStorageService.loadAsResource(storedFileName);
        String contentType = localContentMaterialStorageService.resolveContentType(storedFileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Video>> createContent(@RequestBody Video content) {
        try {
            Video created = videoService.createVideo(content);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("Content created", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Video>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Video>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Video>internalError(e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Video>> createContentWithMaterials(
            @RequestPart("content") Video content,
            @RequestPart(value = "materials", required = false) MultipartFile[] materials) {
        try {
            Video created = videoService.createVideo(content, materials);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("Content created", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Video>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Video>internalError(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Video>internalError(e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @PostMapping(value = "/{id}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Video>> addMaterials(
            @PathVariable Integer id,
            @RequestPart(value = "materials", required = false) MultipartFile[] materials) {
        try {
            Video updated = videoService.addMaterials(id, materials);
            return ResponseEntity.ok(ApiResponse.ok("Materials added", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Video>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Video>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Video>internalError(e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @DeleteMapping("/{id}/materials/{materialId}")
    public ResponseEntity<ApiResponse<Video>> deleteMaterial(
            @PathVariable Integer id,
            @PathVariable Integer materialId) {
        try {
            Video updated = videoService.deleteMaterial(id, materialId);
            return ResponseEntity.ok(ApiResponse.ok("Material deleted", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Video>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Video>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Video>internalError(e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Video>> updateContent(@PathVariable Integer id, @RequestBody Video content) {
        try {
            Video updated = videoService.updateVideo(id, content);
            return ResponseEntity.ok(ApiResponse.ok("Content updated", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Video>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Video>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Video>internalError(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContent(@PathVariable Integer id) {
        try {
            videoService.deleteVideo(id);
            return ResponseEntity.ok(ApiResponse.<Void>ok("Content deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>internalError(e.getMessage()));
        }
    }

    @PostMapping("/stats/record")
    public ResponseEntity<ApiResponse<VideoStats>> recordContentStats(@RequestBody VideoStatsRequest request) {
        try {
            VideoStats stats = videoStatsService.recordStats(request);
            return ResponseEntity.ok(ApiResponse.ok("Content stats recorded", stats));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<VideoStats>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<VideoStats>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<VideoStats>internalError(e.getMessage()));
        }
    }
}
