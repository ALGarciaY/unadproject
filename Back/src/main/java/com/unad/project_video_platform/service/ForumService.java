package com.unad.project_video_platform.service;

import com.unad.project_video_platform.dto.ForumConversationResponse;
import com.unad.project_video_platform.dto.NotificationItemResponse;
import com.unad.project_video_platform.dto.NotificationSummaryResponse;
import com.unad.project_video_platform.dto.QuestionRequest;
import com.unad.project_video_platform.entity.Conversation;
import com.unad.project_video_platform.entity.Question;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.entity.Video;
import com.unad.project_video_platform.repository.ConversationRepository;
import com.unad.project_video_platform.repository.QuestionRepository;
import com.unad.project_video_platform.repository.UserRepository;
import com.unad.project_video_platform.repository.VideoRepository;
import com.unad.project_video_platform.service.impl.IForumService;
import com.unad.project_video_platform.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@Service
public class ForumService implements IForumService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentUser currentUser;

    public List<ForumConversationResponse> getAllConversations() {
        return conversationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toForumConversationResponse)
                .toList();
    }

    public List<Question> getQuestionsByContent(Integer contentId) {
        List<Integer> conversationIds = conversationRepository.findByContentIdOrderByCreatedAtDesc(contentId)
                .stream()
                .map(Conversation::getId)
                .toList();

        if (conversationIds.isEmpty()) {
            return List.of();
        }

        return questionRepository.findByConversationIdInOrderByCreatedAtAsc(conversationIds);
    }

    public NotificationSummaryResponse getNotificationSummary(Integer userId) {
        requireSelf(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        int contentQuestionCount = 0;
        int answerCount = 0;
        List<NotificationItemResponse> items = new ArrayList<>();

        for (Conversation conversation : conversationRepository.findAllByOrderByCreatedAtDesc()) {
            List<Question> questions = questionRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            if (questions.isEmpty()) {
                continue;
            }

            boolean ownsContent = conversation.getContent() != null
                    && conversation.getContent().getCreatedBy() != null
                    && userId.equals(conversation.getContent().getCreatedBy().getId());

            boolean userParticipated = questions.stream()
                    .anyMatch(question -> question.getUser() != null && userId.equals(question.getUser().getId()));

            if (!ownsContent && !userParticipated) {
                continue;
            }

            Question lastMessage = questions.get(questions.size() - 1);
            Integer lastUserId = lastMessage.getUser() != null ? lastMessage.getUser().getId() : null;
            if (userId.equals(lastUserId)) {
                continue;
            }

            boolean isPending = user.getLastSeenNotificationsAt() == null
                    || lastMessage.getCreatedAt().isAfter(user.getLastSeenNotificationsAt());

            if (ownsContent) {
                if (isPending) {
                    contentQuestionCount++;
                }
                items.add(toNotificationItem(conversation, lastMessage, "CONTENT_QUESTION"));
            } else {
                if (isPending) {
                    answerCount++;
                }
                items.add(toNotificationItem(conversation, lastMessage, "ANSWER"));
            }
        }

        return new NotificationSummaryResponse(
                contentQuestionCount,
                answerCount,
                contentQuestionCount + answerCount,
                items);
    }

    @Transactional
    public void markNotificationsSeen(Integer userId) {
        requireSelf(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        user.setLastSeenNotificationsAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public Question createQuestion(QuestionRequest request) {
        validateRequest(request);

        Video content = videoRepository.findById(request.getContentId())
                .orElseThrow(() -> new RuntimeException("Contenido no encontrado con id: " + request.getContentId()));
        User user = currentUser.require();

        Conversation conversation = conversationRepository
                .findFirstByContentIdOrderByCreatedAtAsc(content.getId())
                .orElseGet(() -> createConversation(content, user, request.getTitle()));

        Question question = new Question();
        question.setConversation(conversation);
        question.setUser(user);
        question.setDescription(request.getDescription().trim());

        return questionRepository.save(question);
    }

    private void requireSelf(Integer userId) {
        if (!currentUser.require().getId().equals(userId)) {
            throw new AccessDeniedException("No puedes consultar ni modificar las notificaciones de otro usuario");
        }
    }

    private Conversation createConversation(Video content, User user, String requestedTitle) {
        Conversation conversation = new Conversation();
        conversation.setContent(content);
        conversation.setCreatedBy(user);
        conversation.setTitle(resolveConversationTitle(requestedTitle, content.getTitle()));
        return conversationRepository.save(conversation);
    }

    private ForumConversationResponse toForumConversationResponse(Conversation conversation) {
        List<Question> questions = questionRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        java.util.Map<Integer, User> participantsById = new java.util.LinkedHashMap<>();
        questions.stream()
                .map(Question::getUser)
                .filter(user -> user != null && user.getId() != null)
                .forEach(user -> participantsById.putIfAbsent(user.getId(), user));

        List<Integer> participantIds = List.copyOf(participantsById.keySet());
        List<User> participants = List.copyOf(participantsById.values());

        return new ForumConversationResponse(conversation, participantIds, questions.size(), participants);
    }

    private NotificationItemResponse toNotificationItem(Conversation conversation, Question lastMessage, String type) {
        return new NotificationItemResponse(
                conversation.getId(),
                conversation.getTitle(),
                type,
                conversation.getContent(),
                lastMessage.getUser(),
                lastMessage.getCreatedAt());
    }

    private String resolveConversationTitle(String requestedTitle, String contentTitle) {
        String title = requestedTitle != null && !requestedTitle.isBlank()
                ? requestedTitle.trim()
                : "Dudas sobre " + contentTitle;

        return title.length() > 200 ? title.substring(0, 200) : title;
    }

    private void validateRequest(QuestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La pregunta es obligatoria");
        }
        if (request.getContentId() == null) {
            throw new IllegalArgumentException("El contenido es obligatorio");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("La descripcion de la pregunta es obligatoria");
        }
    }
}
