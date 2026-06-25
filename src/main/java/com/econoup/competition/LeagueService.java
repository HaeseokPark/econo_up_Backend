package com.econoup.competition;

import com.econoup.progress.StudyDayEntity;
import com.econoup.progress.StudyDayRepository;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final List<String> TIERS = List.of("BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND");
    private final UserRepository userRepository;
    private final StudyDayRepository studyDayRepository;
    private final LeagueResultRepository resultRepository;

    public LeagueService(UserRepository userRepository, StudyDayRepository studyDayRepository,
                         LeagueResultRepository resultRepository) {
        this.userRepository = userRepository;
        this.studyDayRepository = studyDayRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> me(UserEntity user) {
        LocalDate start = weekStart(LocalDate.now(SEOUL));
        String tier = safeTier(user.leagueTier);
        List<Map<String, Object>> ranking = rankingRows(start, start.plusDays(6), tier);
        int rank = rankOf(ranking, user.id);
        int weeklyXp = xpOf(ranking, user.id);
        return Map.ofEntries(
                Map.entry("available", true),
                Map.entry("leagueId", leagueId(start, tier)),
                Map.entry("tier", tier),
                Map.entry("rank", rank),
                Map.entry("weeklyXp", weeklyXp),
                Map.entry("crowns", user.crownCount),
                Map.entry("weekStart", start.toString()),
                Map.entry("resetsAt", start.plusDays(7).atStartOfDay(SEOUL).toInstant().toString()),
                Map.entry("rankingSize", ranking.size())
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ranking(UserEntity user, String leagueId) {
        String tier = safeTier(user.leagueTier);
        LocalDate start = parseWeekStart(leagueId).orElse(weekStart(LocalDate.now(SEOUL)));
        List<Map<String, Object>> ranking = rankingRows(start, start.plusDays(6), tier);
        return Map.of("leagueId", LeagueService.leagueId(start, tier), "tier", tier,
                "ranking", ranking, "myRank", rankOf(ranking, user.id));
    }

    @Transactional
    public Map<String, Object> latestResult(UserEntity user) {
        LocalDate previousStart = weekStart(LocalDate.now(SEOUL)).minusWeeks(1);
        LeagueResultEntity result = resultRepository.findByUser_IdAndWeekStart(user.id, previousStart)
                .orElseGet(() -> finalizePreviousWeek(user, previousStart));
        return Map.ofEntries(
                Map.entry("available", true),
                Map.entry("weekStart", result.weekStart.toString()),
                Map.entry("rank", result.rankValue),
                Map.entry("weeklyXp", result.weeklyXp),
                Map.entry("crownsGained", result.crownsGained),
                Map.entry("previousTier", result.previousTier),
                Map.entry("resultingTier", result.resultingTier),
                Map.entry("promoted", TIERS.indexOf(result.resultingTier) > TIERS.indexOf(result.previousTier)),
                Map.entry("demoted", TIERS.indexOf(result.resultingTier) < TIERS.indexOf(result.previousTier))
        );
    }

    private LeagueResultEntity finalizePreviousWeek(UserEntity user, LocalDate start) {
        String oldTier = safeTier(user.leagueTier);
        List<Map<String, Object>> ranking = rankingRows(start, start.plusDays(6), oldTier);
        int rank = rankOf(ranking, user.id);
        int weeklyXp = xpOf(ranking, user.id);
        String newTier = oldTier;
        if (rank > 0 && rank <= 3) newTier = moveTier(oldTier, 1);
        if (ranking.size() >= 6 && rank > ranking.size() - 3) newTier = moveTier(oldTier, -1);
        int crowns = weeklyXp / 100;
        user.leagueTier = newTier;
        user.crownCount += crowns;
        userRepository.save(user);
        LeagueResultEntity result = new LeagueResultEntity(user, start);
        result.rankValue = rank;
        result.weeklyXp = weeklyXp;
        result.crownsGained = crowns;
        result.previousTier = oldTier;
        result.resultingTier = newTier;
        return resultRepository.save(result);
    }

    private List<Map<String, Object>> rankingRows(LocalDate from, LocalDate to, String tier) {
        Map<Long, Integer> xp = new HashMap<>();
        for (StudyDayEntity day : studyDayRepository.findByLocalDateBetween(from, to)) {
            xp.merge(day.user.id, day.xpGained, Integer::sum);
        }
        List<UserEntity> users = userRepository.findTop100ByOrderByTotalXpDesc().stream()
                .filter(user -> user.id != null)
                .filter(user -> Objects.equals(tier, safeTier(user.leagueTier)))
                .limit(20)
                .toList();
        List<UserEntity> sorted = new ArrayList<>(users);
        sorted.sort(Comparator.comparingInt((UserEntity user) -> xp.getOrDefault(user.id, 0)).reversed()
                .thenComparingLong(user -> user.id));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            UserEntity member = sorted.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", i + 1);
            row.put("userId", member.id);
            row.put("nickname", member.nickname == null ? "" : member.nickname);
            row.put("characterId", member.equippedCharacterId == null ? "" : member.equippedCharacterId);
            row.put("weeklyXp", xp.getOrDefault(member.id, 0));
            row.put("crowns", member.crownCount);
            rows.add(row);
        }
        return rows;
    }

    private int rankOf(List<Map<String, Object>> ranking, Long userId) {
        return ranking.stream().filter(row -> Objects.equals(row.get("userId"), userId))
                .map(row -> (Integer) row.get("rank")).findFirst().orElse(0);
    }

    private int xpOf(List<Map<String, Object>> ranking, Long userId) {
        return ranking.stream().filter(row -> Objects.equals(row.get("userId"), userId))
                .map(row -> (Integer) row.get("weeklyXp")).findFirst().orElse(0);
    }

    private String moveTier(String tier, int delta) {
        int current = Math.max(0, TIERS.indexOf(safeTier(tier)));
        return TIERS.get(Math.max(0, Math.min(TIERS.size() - 1, current + delta)));
    }

    private String safeTier(String tier) {
        return TIERS.contains(tier) ? tier : "BRONZE";
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static String leagueId(LocalDate start, String tier) {
        return start + "_" + tier;
    }

    private Optional<LocalDate> parseWeekStart(String value) {
        try {
            return Optional.of(LocalDate.parse(value.substring(0, 10)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
