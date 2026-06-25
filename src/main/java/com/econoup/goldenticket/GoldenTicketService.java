package com.econoup.goldenticket;

import com.econoup.common.ApiException;
import com.econoup.curriculum.*;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.user.UserEntity;
import com.econoup.wallet.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoldenTicketService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final GoldenTicketRepository ticketRepository;
    private final SessionRepository sessionRepository;
    private final CategoryRepository categoryRepository;
    private final LearningAttemptRepository attemptRepository;
    private final ContentEntitlementRepository entitlementRepository;
    private final QuestionInteractionService json;

    public GoldenTicketService(GoldenTicketRepository ticketRepository, SessionRepository sessionRepository,
                               CategoryRepository categoryRepository, LearningAttemptRepository attemptRepository,
                               ContentEntitlementRepository entitlementRepository, QuestionInteractionService json) {
        this.ticketRepository = ticketRepository;
        this.sessionRepository = sessionRepository;
        this.categoryRepository = categoryRepository;
        this.attemptRepository = attemptRepository;
        this.entitlementRepository = entitlementRepository;
        this.json = json;
    }

    @Transactional
    public Map<String, Object> current(UserEntity user) {
        GoldenTicketEntity ticket = ticketRepository.findFirstByUser_IdOrderByIssuedDateDesc(user.id).orElse(null);
        LocalDate today = LocalDate.now(SEOUL);
        if (ticket != null && ticket.expiresAt.isBefore(Instant.now()) && "AVAILABLE".equals(ticket.status)) {
            ticket.status = "EXPIRED";
        }
        boolean eligible = user.goldenTicketEnabled && user.streakDays >= 7;
        if ((ticket == null || ticket.issuedDate.isBefore(today.minusDays(7))) && eligible) {
            List<Long> previewIds = previewSessions(user);
            if (!previewIds.isEmpty()) {
                ticket = ticketRepository.save(new GoldenTicketEntity(user, today, json.writeJson(previewIds),
                        today.plusDays(7).atStartOfDay(SEOUL).toInstant()));
            }
        }
        if (ticket == null) {
            return Map.of("available", false, "eligible", eligible, "requiredStreakDays", 7,
                    "currentStreakDays", user.streakDays, "ticket", Map.of());
        }
        return Map.of("available", "AVAILABLE".equals(ticket.status), "eligible", eligible,
                "ticket", ticketPayload(ticket));
    }

    @Transactional
    public Map<String, Object> activate(UserEntity user, Long ticketId) {
        GoldenTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GOLDEN_TICKET_NOT_FOUND", "Golden ticket not found."));
        if (!ticket.user.id.equals(user.id)) throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Ticket belongs to another user.");
        if (!"AVAILABLE".equals(ticket.status) || ticket.expiresAt.isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "GOLDEN_TICKET_UNAVAILABLE", "Golden ticket is unavailable.");
        }
        List<Long> sessionIds = json.readLongList(ticket.previewSessionIdsJson);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        for (Long sessionId : sessionIds) {
            entitlementRepository.findByUser_IdAndContentTypeAndContentKey(user.id, "SESSION", String.valueOf(sessionId))
                    .orElseGet(() -> entitlementRepository.save(new ContentEntitlementEntity(
                            user, "SESSION", String.valueOf(sessionId), expiresAt)));
        }
        ticket.status = "ACTIVATED";
        ticket.activatedAt = Instant.now();
        return Map.of("ticketId", ticket.id, "activated", true, "sessionIds", sessionIds,
                "accessExpiresAt", expiresAt.toString());
    }

    private List<Long> previewSessions(UserEntity user) {
        for (CategoryEntity category : categoryRepository.findAllByOrderBySortOrderAsc()) {
            for (SessionEntity session : sessionRepository.findOrderedByCategory(category.code)) {
                if (!attemptRepository.existsByUser_IdAndSession_IdAndStatus(user.id, session.id, "COMPLETED")) {
                    List<SessionEntity> sessions = sessionRepository.findByStage_IdOrderBySequenceAsc(session.stage.id);
                    return sessions.stream().limit(3).map(item -> item.id).toList();
                }
            }
        }
        return List.of();
    }

    private Map<String, Object> ticketPayload(GoldenTicketEntity ticket) {
        List<Map<String, Object>> sessions = json.readLongList(ticket.previewSessionIdsJson).stream()
                .map(sessionRepository::findById).flatMap(Optional::stream)
                .map(session -> Map.<String, Object>of("sessionId", session.id, "title", session.title,
                        "stageId", session.stage.id, "stageTitle", session.stage.title,
                        "categoryCode", session.stage.unit.category.code)).toList();
        return Map.of("id", ticket.id, "title", "골든 티켓", "status", ticket.status,
                "previewStages", sessions, "expiresAt", ticket.expiresAt.toString());
    }
}
