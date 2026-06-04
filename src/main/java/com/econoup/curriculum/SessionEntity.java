package com.econoup.curriculum;

import jakarta.persistence.*;

@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_sessions_code", columnList = "code", unique = true),
        @Index(name = "idx_sessions_stage_sequence", columnList = "stage_id,sequence")
})
public class SessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id")
    public StageEntity stage;

    @Column(nullable = false, unique = true)
    public String code;

    @Column(nullable = false)
    public int sequence;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String title;

    protected SessionEntity() {
    }

    public SessionEntity(StageEntity stage, String code, int sequence, String type, String title) {
        this.stage = stage;
        this.code = code;
        this.sequence = sequence;
        this.type = type;
        this.title = title;
    }
}
