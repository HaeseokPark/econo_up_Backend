package com.econoup.curriculum;

import jakarta.persistence.*;

@Entity
@Table(name = "questions", indexes = @Index(name = "idx_questions_session_sequence", columnList = "session_id,sequence"))
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    public SessionEntity session;

    @Column(nullable = false)
    public int sequence;

    @Column(nullable = false)
    public String type;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String prompt;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    public String payloadJson;

    @Column(name = "answer_json", columnDefinition = "TEXT")
    public String answerJson;

    @Column(columnDefinition = "TEXT")
    public String explanation;

    @Column(name = "highlight_text", columnDefinition = "TEXT")
    public String highlightText;

    protected QuestionEntity() {
    }

    public QuestionEntity(
            SessionEntity session,
            int sequence,
            String type,
            String prompt,
            String payloadJson,
            String answerJson,
            String explanation,
            String highlightText
    ) {
        this.session = session;
        this.sequence = sequence;
        this.type = type;
        this.prompt = prompt;
        this.payloadJson = payloadJson;
        this.answerJson = answerJson;
        this.explanation = explanation;
        this.highlightText = highlightText;
    }
}
