package com.econoup.social;

import com.econoup.common.ApiException;
import com.econoup.progress.StudyDayEntity;
import com.econoup.progress.StudyDayRepository;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import com.econoup.wallet.PurchaseEntity;
import com.econoup.wallet.PurchaseRepository;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final PokeRepository pokeRepository;
    private final PurchaseRepository purchaseRepository;
    private final StudyDayRepository studyDayRepository;

    public SocialService(UserRepository userRepository, FriendshipRepository friendshipRepository,
                         PokeRepository pokeRepository, PurchaseRepository purchaseRepository,
                         StudyDayRepository studyDayRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.pokeRepository = pokeRepository;
        this.purchaseRepository = purchaseRepository;
        this.studyDayRepository = studyDayRepository;
    }

    @Transactional
    public Map<String, Object> friends(UserEntity user) {
        int claimed = claimPokeRewards(user);
        List<Map<String, Object>> friends = acceptedFriends(user).stream().map(friend -> userPayload(user, friend)).toList();
        List<Map<String, Object>> pending = friendshipRepository
                .findByReceiver_IdAndStatusOrderByCreatedAtDesc(user.id, "PENDING").stream()
                .map(request -> Map.<String, Object>of(
                        "requestId", request.id,
                        "user", basicUser(request.requester),
                        "createdAt", request.createdAt.toString()))
                .toList();
        return Map.of("friends", friends, "pendingRequests", pending, "claimedPokeRewards", claimed);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(UserEntity user, String nickname) {
        if (nickname == null || nickname.trim().length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "Enter at least two nickname characters.");
        }
        List<Map<String, Object>> users = userRepository
                .findTop20ByNicknameContainingIgnoreCaseAndIdNot(nickname.trim(), user.id).stream()
                .map(found -> userPayload(user, found)).toList();
        return Map.of("query", nickname.trim(), "users", users);
    }

    @Transactional
    public Map<String, Object> request(UserEntity user, Long receiverId) {
        if (receiverId == null || receiverId.equals(user.id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FRIEND", "A different userId is required.");
        }
        UserEntity receiver = findUser(receiverId);
        FriendshipEntity existing = friendshipRepository.findPair(user.id, receiverId).orElse(null);
        if (existing != null && !"REJECTED".equals(existing.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "FRIEND_REQUEST_EXISTS", "A friendship or request already exists.");
        }
        FriendshipEntity request = existing == null ? new FriendshipEntity(user, receiver) : existing;
        request.requester = user;
        request.receiver = receiver;
        request.status = "PENDING";
        request.createdAt = Instant.now();
        request.respondedAt = null;
        friendshipRepository.save(request);
        return Map.of("requestId", request.id, "status", request.status, "receiver", basicUser(receiver));
    }

    @Transactional
    public Map<String, Object> respond(UserEntity user, Long requestId, boolean accept) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND", "Friend request not found."));
        if (!request.receiver.id.equals(user.id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the receiver can respond to this request.");
        }
        if (!"PENDING".equals(request.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_ALREADY_RESPONDED", "Friend request was already processed.");
        }
        request.status = accept ? "ACCEPTED" : "REJECTED";
        request.respondedAt = Instant.now();
        return Map.of("requestId", request.id, "status", request.status);
    }

    @Transactional
    public Map<String, Object> delete(UserEntity user, Long friendId) {
        FriendshipEntity friendship = friendshipRepository.findPair(user.id, friendId)
                .filter(item -> "ACCEPTED".equals(item.status))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FRIEND_NOT_FOUND", "Friendship not found."));
        friendshipRepository.delete(friendship);
        return Map.of("friendId", friendId, "deleted", true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> feed(UserEntity user) {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<Long, Integer> weeklyXp = new HashMap<>();
        for (StudyDayEntity day : studyDayRepository.findByLocalDateBetween(monday, today)) {
            weeklyXp.merge(day.user.id, day.xpGained, Integer::sum);
        }
        List<UserEntity> members = new ArrayList<>(acceptedFriends(user));
        members.add(user);
        members.sort(Comparator.comparingInt((UserEntity member) -> weeklyXp.getOrDefault(member.id, 0)).reversed());
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            UserEntity member = members.get(i);
            items.add(Map.of("rank", i + 1, "user", basicUser(member),
                    "weeklyXp", weeklyXp.getOrDefault(member.id, 0), "isMe", member.id.equals(user.id)));
        }
        return Map.of("weekStart", monday.toString(), "items", items, "hasMore", false, "nextCursor", "");
    }

    @Transactional
    public Map<String, Object> poke(UserEntity user, Long friendId) {
        UserEntity friend = findUser(friendId);
        friendshipRepository.findPair(user.id, friendId)
                .filter(item -> "ACCEPTED".equals(item.status))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NOT_FRIEND", "Only friends can be poked."));
        LocalDate today = LocalDate.now(SEOUL);
        if (pokeRepository.existsBySender_IdAndReceiver_IdAndLocalDate(user.id, friendId, today)) {
            throw new ApiException(HttpStatus.CONFLICT, "POKE_ALREADY_SENT", "This friend was already poked today.");
        }
        PokeEntity poke = pokeRepository.save(new PokeEntity(user, friend, today));
        user.billBalance++;
        userRepository.save(user);
        purchaseRepository.save(new PurchaseEntity(user, "POKE_REWARD", 1, "poke:" + poke.id));
        return Map.of("pokeId", poke.id, "friendId", friendId, "sent", true,
                "senderRewardBills", 1, "billBalance", user.billBalance);
    }

    private int claimPokeRewards(UserEntity user) {
        List<PokeEntity> pending = pokeRepository.findByReceiver_IdAndReceiverRewardClaimedAtIsNull(user.id);
        if (pending.isEmpty()) return 0;
        for (PokeEntity poke : pending) poke.receiverRewardClaimedAt = Instant.now();
        user.billBalance += pending.size();
        userRepository.save(user);
        purchaseRepository.save(new PurchaseEntity(user, "POKE_RECEIVED_REWARD", pending.size(), "pending poke rewards"));
        return pending.size();
    }

    private List<UserEntity> acceptedFriends(UserEntity user) {
        return friendshipRepository.findAccepted(user.id).stream()
                .map(item -> item.requester.id.equals(user.id) ? item.receiver : item.requester).toList();
    }

    private Map<String, Object> userPayload(UserEntity me, UserEntity user) {
        String status = friendshipRepository.findPair(me.id, user.id).map(item -> item.status).orElse("NONE");
        return Map.of("id", user.id, "nickname", nullToEmpty(user.nickname),
                "characterId", nullToEmpty(user.equippedCharacterId), "friendStatus", status,
                "streakDays", user.streakDays, "totalXp", user.totalXp);
    }

    private Map<String, Object> basicUser(UserEntity user) {
        return Map.of("id", user.id, "nickname", nullToEmpty(user.nickname),
                "characterId", nullToEmpty(user.equippedCharacterId));
    }

    private UserEntity findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
