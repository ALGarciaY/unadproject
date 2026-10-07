package com.unad.project_video_platform.service;

import com.unad.project_video_platform.dto.ContentViewSummary;
import com.unad.project_video_platform.dto.VideoStatsRequest;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.entity.Video;
import com.unad.project_video_platform.entity.VideoStats;
import com.unad.project_video_platform.repository.UserRepository;
import com.unad.project_video_platform.repository.VideoRepository;
import com.unad.project_video_platform.repository.VideoStatsRepository;
import com.unad.project_video_platform.service.impl.IVideoStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VideoStatsService implements IVideoStatsService {

    @Autowired
    private VideoStatsRepository videoStatsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private com.unad.project_video_platform.security.CurrentUser currentUser;

    @Transactional
    public VideoStats recordStats(VideoStatsRequest request) {
        if (request == null || request.getContentId() == null) {
            throw new IllegalArgumentException("El usuario y el contenido son obligatorios");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer periodYear = now.getYear();
        Integer periodMonth = now.getMonthValue();
        Integer secondsToAdd = Math.max(request.getWatchTimeSeconds() != null ? request.getWatchTimeSeconds() : 0, 0);
        boolean countView = Boolean.TRUE.equals(request.getCountView());

        User user = currentUser.require();
        Integer userId = user.getId();
        Video content = videoRepository.findById(request.getContentId())
                .orElseThrow(() -> new RuntimeException("Contenido no encontrado con id: " + request.getContentId()));

        VideoStats stats = videoStatsRepository
                .findByUserIdAndContentIdAndPeriodYearAndPeriodMonth(
                        userId,
                        request.getContentId(),
                        periodYear,
                        periodMonth
                )
                .orElseGet(() -> {
                    VideoStats created = new VideoStats();
                    created.setUser(user);
                    created.setContent(content);
                    created.setPeriodYear(periodYear);
                    created.setPeriodMonth(periodMonth);
                    created.setTotalViews(0);
                    created.setWatchTimeSeconds(0);
                    created.setFirstViewAt(now);
                    return created;
                });

        if (countView) {
            stats.setTotalViews((stats.getTotalViews() != null ? stats.getTotalViews() : 0) + 1);
        }

        if (secondsToAdd > 0) {
            stats.setWatchTimeSeconds((stats.getWatchTimeSeconds() != null ? stats.getWatchTimeSeconds() : 0) + secondsToAdd);
        }

        if (stats.getFirstViewAt() == null) {
            stats.setFirstViewAt(now);
        }
        stats.setLastViewAt(now);

        return videoStatsRepository.save(stats);
    }

    public List<VideoStats> getAllStats() {
        return videoStatsRepository.findAll();
    }

    public List<VideoStats> getStatsByContent(Integer contentId) {
        return videoStatsRepository.findByContentId(contentId);
    }

    public List<VideoStats> getStatsByUser(Integer userId) {
        return videoStatsRepository.findByUserId(userId);
    }

    public List<ContentViewSummary> getViewsByContent() {
        return videoStatsRepository.sumViewsGroupedByContent();
    }
}
