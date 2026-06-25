package com.econoup.dailyconnect;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "article_bookmarks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_article_bookmark", columnNames = {"user_id", "article_id"})
})
public class ArticleBookmarkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    public DailyArticleEntity article;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    protected ArticleBookmarkEntity() {}

    public ArticleBookmarkEntity(UserEntity user, DailyArticleEntity article) {
        this.user = user;
        this.article = article;
    }
}
