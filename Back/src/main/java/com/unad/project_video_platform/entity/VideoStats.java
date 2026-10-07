package com.unad.project_video_platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "content_id", "period_year", "period_month"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"documentNumber", "email"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false)
    private Video content;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "total_views", nullable = false)
    private Integer totalViews = 0;

    @Column(name = "watch_time_seconds", nullable = false)
    private Integer watchTimeSeconds = 0;

    @Column(name = "first_view_at")
    private LocalDateTime firstViewAt;

    @Column(name = "last_view_at")
    private LocalDateTime lastViewAt;
}
