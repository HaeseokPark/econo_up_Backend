package com.econoup.wallet;

import com.econoup.common.ApiException;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ContentEntitlementRepository entitlementRepository;

    public WalletService(UserRepository userRepository, PurchaseRepository purchaseRepository,
                         ContentEntitlementRepository entitlementRepository) {
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.entitlementRepository = entitlementRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> balance(UserEntity user) {
        refreshHearts(user);
        return walletPayload(user);
    }

    @Transactional
    public boolean consumeHeart(UserEntity user) {
        refreshHearts(user);
        if (isUnlimited(user)) return false;
        if (user.heartCurrent <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "HEART_EMPTY", "No hearts remain. Refill a heart before continuing.");
        }
        user.heartCurrent--;
        if (user.heartCurrent < user.heartMax && user.heartRefillAt == null) {
            user.heartRefillAt = Instant.now().plus(Duration.ofMinutes(30));
        }
        userRepository.save(user);
        return true;
    }

    @Transactional
    public Map<String, Object> refillHeart(UserEntity user) {
        refreshHearts(user);
        if (user.heartCurrent >= user.heartMax) {
            throw new ApiException(HttpStatus.CONFLICT, "HEART_FULL", "Hearts are already full.");
        }
        debit(user, 1, "HEART_REFILL");
        user.heartCurrent++;
        if (user.heartCurrent >= user.heartMax) user.heartRefillAt = null;
        userRepository.save(user);
        return Map.of("wallet", walletPayload(user), "purchased", "HEART_REFILL");
    }

    @Transactional
    public Map<String, Object> purchaseUnlimitedHeart(UserEntity user) {
        debit(user, 3, "UNLIMITED_HEART_24H");
        Instant base = user.unlimitedHeartUntil != null && user.unlimitedHeartUntil.isAfter(Instant.now())
                ? user.unlimitedHeartUntil : Instant.now();
        user.unlimitedHeartUntil = base.plus(Duration.ofHours(24));
        userRepository.save(user);
        return Map.of("wallet", walletPayload(user), "purchased", "UNLIMITED_HEART_24H");
    }

    @Transactional
    public Map<String, Object> purchaseReviveTicket(UserEntity user) {
        debit(user, 2, "STREAK_REVIVE_TICKET");
        user.streakReviveTicketBalance++;
        userRepository.save(user);
        return Map.of("wallet", walletPayload(user), "purchased", "STREAK_REVIVE_TICKET");
    }

    @Transactional
    public Map<String, Object> reviveStreak(UserEntity user) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (user.streakReviveTicketBalance <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIVE_TICKET_REQUIRED", "A streak revive ticket is required.");
        }
        if (today.equals(user.streakReviveUsedDate)) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIVE_ALREADY_USED", "A revive was already used today.");
        }
        if (user.lastStudyDate == null || !user.lastStudyDate.isBefore(today.minusDays(1))) {
            throw new ApiException(HttpStatus.CONFLICT, "STREAK_NOT_BROKEN", "The streak is not eligible for revival.");
        }
        user.streakReviveTicketBalance--;
        user.streakReviveUsedDate = today;
        user.lastStudyDate = today.minusDays(1);
        user.streakDays = Math.max(1, user.streakDays);
        userRepository.save(user);
        return Map.of("revived", true, "streakDays", user.streakDays, "wallet", walletPayload(user));
    }

    @Transactional
    public Map<String, Object> purchaseUnlock(UserEntity user, UnlockRequest request) {
        if (request == null || request.contentType() == null || request.contentKey() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_UNLOCK", "contentType and contentKey are required.");
        }
        int price = request.priceBills() == null ? 3 : request.priceBills();
        if (price <= 0 || price > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRICE", "priceBills must be between 1 and 100.");
        }
        ContentEntitlementEntity existing = entitlementRepository
                .findByUser_IdAndContentTypeAndContentKey(user.id, request.contentType(), request.contentKey())
                .orElse(null);
        if (existing != null && (existing.expiresAt == null || existing.expiresAt.isAfter(Instant.now()))) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_UNLOCKED", "Content is already unlocked.");
        }
        debit(user, price, "UNLOCK:" + request.contentType() + ":" + request.contentKey());
        Instant expiresAt = request.durationHours() == null ? null : Instant.now().plus(Duration.ofHours(request.durationHours()));
        if (existing == null) {
            entitlementRepository.save(new ContentEntitlementEntity(user, request.contentType(), request.contentKey(), expiresAt));
        } else {
            existing.grantedAt = Instant.now();
            existing.expiresAt = expiresAt;
        }
        return Map.of("unlocked", true, "contentType", request.contentType(), "contentKey", request.contentKey(),
                "wallet", walletPayload(user));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> unlocks(UserEntity user, String contentType) {
        List<Map<String, Object>> items = entitlementRepository.findByUser_IdAndContentType(user.id, contentType).stream()
                .filter(item -> item.expiresAt == null || item.expiresAt.isAfter(Instant.now()))
                .map(item -> Map.<String, Object>of(
                        "contentType", item.contentType,
                        "contentKey", item.contentKey,
                        "grantedAt", item.grantedAt.toString(),
                        "expiresAt", item.expiresAt == null ? "" : item.expiresAt.toString()))
                .toList();
        return Map.of("items", items);
    }

    @Transactional
    public Map<String, Object> grant(UserEntity user, WalletAmountRequest request) {
        int amount = positiveAmount(request);
        user.billBalance += amount;
        userRepository.save(user);
        purchaseRepository.save(new PurchaseEntity(user, "DEV_GRANT", amount, memo(request)));
        return Map.of(
                "wallet", walletPayload(user),
                "transaction", Map.of("type", "DEV_GRANT", "amount", amount)
        );
    }

    @Transactional
    public Map<String, Object> spend(UserEntity user, WalletAmountRequest request) {
        int amount = positiveAmount(request);
        debit(user, amount, memo(request).isBlank() ? "SPEND" : memo(request));
        return Map.of(
                "wallet", walletPayload(user),
                "transaction", Map.of("type", "SPEND", "amount", amount)
        );
    }

    private int positiveAmount(WalletAmountRequest request) {
        if (request == null || request.amount() == null || request.amount() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero.");
        }
        return request.amount();
    }

    private String memo(WalletAmountRequest request) {
        return request == null || request.memo() == null ? "" : request.memo();
    }

    private Map<String, Object> walletPayload(UserEntity user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("billBalance", user.billBalance);
        payload.put("currency", "BILL");
        payload.put("hearts", Map.of(
                "current", user.heartCurrent,
                "max", user.heartMax,
                "nextRefillAt", user.heartRefillAt == null ? "" : user.heartRefillAt.toString(),
                "unlimited", isUnlimited(user),
                "unlimitedUntil", user.unlimitedHeartUntil == null ? "" : user.unlimitedHeartUntil.toString()
        ));
        payload.put("streakReviveTicketBalance", user.streakReviveTicketBalance);
        return payload;
    }

    private void debit(UserEntity user, int amount, String memo) {
        if (user.billBalance < amount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BILL_NOT_ENOUGH", "Bill balance is not enough.");
        }
        user.billBalance -= amount;
        userRepository.save(user);
        purchaseRepository.save(new PurchaseEntity(user, "SPEND", -amount, memo));
    }

    private boolean isUnlimited(UserEntity user) {
        return user.unlimitedHeartUntil != null && user.unlimitedHeartUntil.isAfter(Instant.now());
    }

    private void refreshHearts(UserEntity user) {
        Instant now = Instant.now();
        if (user.heartCurrent >= user.heartMax) {
            user.heartRefillAt = null;
            return;
        }
        if (user.heartRefillAt == null) {
            user.heartRefillAt = now.plus(Duration.ofMinutes(30));
            userRepository.save(user);
            return;
        }
        if (!user.heartRefillAt.isAfter(now)) {
            long refillCount = 1 + Duration.between(user.heartRefillAt, now).toMinutes() / 30;
            user.heartCurrent = Math.min(user.heartMax, user.heartCurrent + (int) refillCount);
            user.heartRefillAt = user.heartCurrent >= user.heartMax
                    ? null : user.heartRefillAt.plus(Duration.ofMinutes(refillCount * 30));
            userRepository.save(user);
        }
    }

    public record WalletAmountRequest(Integer amount, String memo) {
    }

    public record UnlockRequest(String contentType, String contentKey, Integer priceBills, Long durationHours) {
    }
}
