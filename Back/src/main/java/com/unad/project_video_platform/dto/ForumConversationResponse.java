package com.unad.project_video_platform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unad.project_video_platform.entity.Conversation;
import com.unad.project_video_platform.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumConversationResponse {
    private Conversation conversation;
    private List<Integer> participantIds;
    private Integer questionCount;
    @JsonIgnoreProperties({"documentNumber", "email"})
    private List<User> participants;
}
