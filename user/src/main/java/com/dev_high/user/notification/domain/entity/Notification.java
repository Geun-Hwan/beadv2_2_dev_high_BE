package com.dev_high.user.notification.domain.entity;

import com.dev_high.common.annotation.CustomGeneratedId;
import com.dev_high.common.type.NotificationCategory;
import com.dev_high.user.notification.infrastructure.converter.BooleanToYNConverter;
import io.jsonwebtoken.lang.Assert;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Optional;


@Table(name = "notification", schema = "user")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification{

    @Id
    @Column(name = "id", length = 20)
    @CustomGeneratedId(method = "notification")
    private String id;

    /* 추후 user 테이블의 외래 키 관계 명확화를 할수 있음
    * @ManyToOne(fetch = FetchType.LAZY)
    * @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false) // FK 매핑
    * private User user;
    * */
    @Schema(description = "사용자 ID")
    @Column(name = "user_id", length = 20, nullable = false)
    private String userId;

    @Schema(description = "알림 카테고리")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private NotificationCategory category;

    @Schema(description = "알림 타입")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private NotificationCategory.Type type;

    @Schema(description = "제목")
    @Column(name = "title", length = 50, nullable = false)
    private String title;

    @Schema(description = "내용")
    @Column(name = "content", length = 50)
    private String content;

    @Schema(description = "상세보기 연결 URL")
    @Column(name = "related_url", length = 100)
    private String relatedUrl;

    @Schema(description = "확인 여부")
    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "read_yn", nullable = false, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private Boolean readYn;

    @Schema(description = "만료일자")
    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    @Schema(description = "생성일시")
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Schema(description = "생성자")
    @Column(name = "created_by", length = 20, nullable = false, updatable = false)
    private String createdBy;

    @Schema(description = "수정일시")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Schema(description = "수정자")
    @Column(name = "updated_by", length = 20, nullable = false)
    private String updatedBy;

    @Builder
    public Notification(String userId, NotificationCategory category, NotificationCategory.Type type, String title, String content, String relatedUrl, Boolean readYn) {
        this.userId = userId;
        this.category = category;
        this.type = type;
        this.title = title;
        this.content = content;
        this.relatedUrl = relatedUrl;
        this.readYn = Optional.of(readYn)
                .orElse(false);
        this.createdBy = userId;
        this.updatedBy = userId;
    }

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.expiredAt = now.plusDays(30);
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = userId;
        this.updatedBy  = userId;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = userId;
    }

    public static Notification create(
        String userId,
        NotificationCategory.Type type,
        String title,
        String content,
        String relatedUrl
    ) {
        Assert.notNull(userId, "사용자 ID는 필수입니다.");
        Assert.hasText(title, "제목은 필수입니다.");
        return Notification.builder()
                .userId(userId)
                .category(NotificationCategory.fromType(type))
                .type(type)
                .title(title)
                .content(content)
                .relatedUrl(relatedUrl)
                .readYn(false) // 기본값은 false
                .build();
    }

    public void markAsRead(String userId) {
        this.readYn = true;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = userId;
    }
}
