package com.unad.project_video_platform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemResponse {
    private Integer conversationId;
    private String conversationTitle;
    private String type;
    private Video content;
    @JsonIgnoreProperties({"documentNumber", "email"})
    private User lastMessageBy;
    private LocalDateTime lastMessageAt;
}
