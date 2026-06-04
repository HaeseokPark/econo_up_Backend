package com.econoup.review;

import com.econoup.curriculum.QuestionEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "review_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_item_set_question", columnNames = {"review_set_id", "question_id"})
}, indexes = {
        @Index(name = "idx_review_item_set_sequence", columnList = "review_set_id,sequence")
})
public class ReviewItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_set_id")
    public ReviewSetEntity reviewSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    public QuestionEntity question;

    @Column(nullable = false)
    public int sequence;

    protected ReviewItemEntity() {
    }

    public ReviewItemEntity(ReviewSetEntity reviewSet, QuestionEntity question, int sequence) {
        this.reviewSet = reviewSet;
        this.question = question;
        this.sequence = sequence;
    }
}
