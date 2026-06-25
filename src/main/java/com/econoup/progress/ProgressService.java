package com.econoup.progress;

import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final StudyDayRepository studyDayRepository;
    private final UserRepository userRepository;

    public ProgressService(StudyDayRepository studyDayRepository, UserRepository userRepository) {
        this.studyDayRepository = studyDayRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(UserEntity user, int xpGained, int studyMinutes, boolean sessionCompleted) {
        LocalDate today = LocalDate.now(SEOUL);
        StudyDayEntity day = studyDayRepository.findByUser_IdAndLocalDate(user.id, today)
                .orElseGet(() -> new StudyDayEntity(user, today));
        day.xpGained += Math.max(0, xpGained);
        day.studyMinutes += Math.max(0, studyMinutes);
        if (sessionCompleted) day.sessionsCompleted++;
        studyDayRepository.save(day);

        if (user.lastStudyDate == null) {
            user.streakDays = 1;
        } else if (user.lastStudyDate.equals(today.minusDays(1))) {
            user.streakDays++;
        } else if (!user.lastStudyDate.equals(today)) {
            user.streakDays = 1;
        }
        user.lastStudyDate = today;
        userRepository.save(user);
    }
}
