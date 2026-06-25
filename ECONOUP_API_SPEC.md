# Econo-up Backend API Specification

## 0. Document status

| Item | Value |
| --- | --- |
| Version | `2026-06-20-final-backend` |
| Base URL | `/api/v1` |
| Source of truth | Current Spring Boot implementation and final 126-page screen design |
| Authentication | `Authorization: Bearer {accessToken}` except login/refresh |
| JSON naming | `lowerCamelCase` |
| Time | ISO-8601 UTC string; study-day calculation uses `Asia/Seoul` |

This document describes the response actually produced by the current code. Frontend code must not read `questions.answerJson`; it is server-only grading data.

### 0.1 Completion boundary

- Implemented: onboarding, level test, home, curriculum, learning/grading, review, streak/records, wallet/hearts, golden tickets, 11 simulation quests, news/terms/quiz/bookmarks, characters, social/pokes, league, asynchronous battles, profile/settings.
- External setup required: Kakao/Apple OAuth, App Store/Google Play receipt verification and settlement, live news ingestion, push delivery, public terms/privacy URLs.
- A missing external setup returns a configuration error; the backend does not return fake success.

## 1. Common contract

### 1.1 Success

```json
{
  "success": true,
  "data": {}
}
```

### 1.2 Failure

```json
{
  "success": false,
  "error": {
    "code": "CONTENT_NOT_FOUND",
    "message": "Session not found."
  }
}
```

Important status codes: `400` invalid input, `401` invalid token, `403` ownership/lock failure, `404` missing data, `409` invalid state or duplicate request, `501` external provider not configured.

### 1.3 Question answer shape

One of the following fields is sent in `answer`. The same contract is used by learning, level tests, reviews, simulations, and battles.

```json
{ "choiceIds": ["choice_a"] }
{ "orderedItemIds": ["item_1", "item_2"] }
{ "numberValue": 120000 }
{ "answerText": "기준금리" }
```

### 1.4 LearningQuestion

```json
{
  "id": 101,
  "type": "SINGLE_CHOICE",
  "prompt": "기준금리가 오르면 일반적으로 어떤 변화가 생길까요?",
  "context": {
    "categoryCode": "ECONOMY",
    "unitTitle": "금리 기초",
    "stageTitle": "기준금리 이해"
  },
  "choices": [
    { "id": "choice_a", "text": "대출 이자 부담이 커질 수 있다" },
    { "id": "choice_b", "text": "모든 물가가 즉시 내려간다" }
  ],
  "resource": { "type": "TEXT", "text": "한국은행은 기준금리를 결정합니다." },
  "answerPolicy": { "revealImmediately": true }
}
```

`answer`, `answerJson`, and `correctAnswer` are never included before submission.

## 2. Auth and account

| Method | Path | Request / Note |
| --- | --- | --- |
| `POST` | `/auth/google/login` | `{ "idToken": "...", "termsAgreed": true }` |
| `POST` | `/auth/social/login` | `{ "provider":"GOOGLE", "providerToken":"...", "termsAgreed":true }` |
| `POST` | `/auth/token/refresh` | `{ "refreshToken":"..." }` |
| `POST` | `/auth/logout` | Client discards tokens |
| `GET` | `/users/me` | Current profile and onboarding state |
| `DELETE` | `/users/me` | Anonymizes and disables the account |

Login response:

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt",
    "refreshToken": "jwt",
    "isNewUser": true,
    "nextScreen": "ONBOARDING_PROFILE"
  }
}
```

`KAKAO` and `APPLE` currently return `AUTH_PROVIDER_CONFIGURATION_REQUIRED` until provider credentials and callback settings are supplied.

## 3. Onboarding and level test

| Method | Path | Request |
| --- | --- | --- |
| `GET` | `/users/nickname-availability?nickname=예진` | None |
| `GET` | `/onboarding/status` | None |
| `PUT` | `/onboarding/profile` | `{ "nickname":"예진", "gender":"FEMALE", "age":25 }` |
| `PUT` | `/onboarding/interests` | `{ "categoryCodes":["ECONOMY","INVESTMENT"] }` |
| `PUT` | `/onboarding/goal` | `{ "goal":"PRACTICAL_FINANCE" }` |
| `PUT` | `/onboarding/study-style` | `{ "frequency":"DAILY", "depth":"LIGHT", "sessionVolume":"THREE" }` |
| `PUT` | `/onboarding/failure-reason` | `{ "reason":"TOO_DIFFICULT" }` |
| `POST` | `/level-tests` | `{ "questionCount":10 }` |
| `POST` | `/level-tests/{testId}/answers` | `{ "questionId":101, "answer":{"choiceIds":["choice_a"]} }` |
| `POST` | `/level-tests/{testId}/complete` | None |
| `POST` | `/level-tests/skip` | None |

Level-test questions use `LearningQuestion`. Each submission is stored in `level_test_answers`; completion stores the score and recommended start result.

## 4. Home and curriculum

### 4.1 `GET /home`

```json
{
  "success": true,
  "data": {
    "user": { "nickname":"예진", "isNewLearner":false },
    "summary": { "streakDays":7, "heartCurrent":2, "heartMax":3, "billBalance":11 },
    "today": {
      "review": { "available":true, "completed":false, "questionCount":5, "answeredCount":1, "ctaPath":"/reviews/today" },
      "dailyConnect": { "available":true, "completed":false, "article":{} }
    },
    "continueLearning": {
      "hasInProgressAttempt":true,
      "attemptId":31,
      "resume":true,
      "session":{},
      "categories":[]
    },
    "recommendedSimulation": {},
    "goldenTicket": {},
    "leaguePreview": {},
    "serverTime":"2026-06-20T08:00:00Z"
  }
}
```

The most recent `IN_PROGRESS` attempt takes priority. Otherwise the next uncompleted session is returned with `resume=false`.

| Method | Path |
| --- | --- |
| `PUT` | `/users/me/home-interests` |
| `GET` | `/curriculum/categories` |
| `GET` | `/curriculum/categories/{categoryCode}/roadmap` |
| `GET` | `/curriculum/units/{unitId}/stages/{stageId}/map` |

Roadmap responses include completed session counts, progress percentages, and lock/current/completed state calculated from the authenticated user's attempts.

## 5. Learning

### 5.1 Start or resume

`POST /learning/sessions/{sessionId}/attempts`

```json
{ "resume": true }
```

```json
{
  "success": true,
  "data": {
    "attemptId":31,
    "session": { "id":11, "code":"ECONOMY-1-1-1", "type":"QUIZ", "title":"금리 퀴즈", "categoryCode":"ECONOMY", "unitId":1, "stageId":1 },
    "progress": { "current":2, "total":5 },
    "question": { "id":102, "type":"SINGLE_CHOICE", "prompt":"...", "choices":[] }
  }
}
```

### 5.2 Submit an answer

`POST /learning/attempts/{attemptId}/answers`

```json
{
  "questionId": 102,
  "answer": { "choiceIds":["choice_a"] },
  "clientAnsweredAt": "2026-06-20T08:01:00Z"
}
```

```json
{
  "success": true,
  "data": {
    "feedback": {
      "correct": false,
      "correctAnswer": { "choiceIds":["choice_b"] },
      "explanation": "기준금리 상승은 대출 금리에 영향을 줄 수 있습니다.",
      "highlightText": "대출 이자 부담",
      "reward": { "xpGained":0, "heartConsumed":1 },
      "heart": { "current":1, "max":3 }
    },
    "progress": { "current":3, "answered":2, "total":5 },
    "nextQuestion": {}
  }
}
```

Grading reads server-only `questions.answer_json`. The submitted JSON, result, and timestamps are upserted in `learning_answers`. A wrong answer consumes one heart unless a 24-hour unlimited pass is active.

### 5.3 Finish or exit

| Method | Path | Result |
| --- | --- | --- |
| `POST` | `/learning/attempts/{attemptId}/exit` | Keeps `IN_PROGRESS` for resume |
| `POST` | `/learning/attempts/{attemptId}/complete` | XP, category growth, next session, stage completion |
| `GET` | `/learning/stages/{stageId}/completion-summary` | Stage summary |

Completion is idempotent: XP is awarded only on the first state transition to `COMPLETED`.

## 6. Review

| Method | Path |
| --- | --- |
| `GET` | `/reviews/today` |
| `POST` | `/reviews/{reviewSetId}/answers` |
| `POST` | `/reviews/{reviewSetId}/complete` |

The daily set uses wrong answers first and recent learning questions next. It is stable per user and KST date.

```json
{
  "success": true,
  "data": {
    "reviewSetId":19,
    "localDate":"2026-06-20",
    "status":"IN_PROGRESS",
    "progress": { "answered":1, "total":5, "percent":20 },
    "question": {}
  }
}
```

Answer feedback contains `correct`, `correctAnswer`, `explanation`, `xpGained`, `progress`, and `nextQuestion`. Review answers are stored in `review_answers` per user set and question.

## 7. Streak, wallet, hearts, and content access

| Method | Path | Request / Price |
| --- | --- | --- |
| `GET` | `/progress/streak` | Current streak and revive eligibility |
| `GET` | `/progress/learning-records` | 90-day calendar and category progress |
| `GET` | `/progress/competency-report` | Category score/level |
| `GET` | `/wallet` | Bills, hearts, passes, revive tickets |
| `POST` | `/wallet/bills/grant` | Dev/event only: `{ "amount":10, "memo":"event" }` |
| `POST` | `/wallet/bills/spend` | Internal/admin use |
| `POST` | `/wallet/hearts/refill` | 1 bill, +1 heart |
| `POST` | `/wallet/hearts/unlimited-pass` | 3 bills, 24 hours |
| `POST` | `/wallet/streak-revive-tickets/purchase` | 2 bills, +1 ticket |
| `POST` | `/wallet/streak/revive` | Uses one ticket, once per day |
| `GET` | `/wallet/unlocks?contentType=SESSION` | Active entitlements |
| `POST` | `/wallet/unlocks/purchase` | See request below |

Wallet response:

```json
{
  "billBalance": 11,
  "currency": "BILL",
  "hearts": {
    "current": 2,
    "max": 3,
    "nextRefillAt": "2026-06-20T08:30:00Z",
    "unlimited": false,
    "unlimitedUntil": ""
  },
  "streakReviveTicketBalance": 1
}
```

Unlock purchase request:

```json
{ "contentType":"SESSION", "contentKey":"451", "priceBills":3, "durationHours":24 }
```

All balance changes are stored in `purchases`; access rights are stored in `content_entitlements`.

## 8. Golden ticket

| Method | Path |
| --- | --- |
| `GET` | `/golden-tickets/current` |
| `POST` | `/golden-tickets/{ticketId}/activate` |

A user with a streak of at least seven days receives an available ticket. Activating it grants 24-hour `SESSION` entitlements for the preview sessions. Issue and activation state are stored in `golden_tickets`.

## 9. Simulations

The seeded catalog contains 11 final-design quests: first real-estate contract, year-end tax, first credit card, youth account, ISA/ETF, overseas stock, IRP, insurance claim, credit score, income tax, and fraud response.

| Method | Path |
| --- | --- |
| `GET` | `/simulations` |
| `POST` | `/simulations/{simulationId}/attempts` |
| `GET` | `/simulation-attempts/{attemptId}/steps/{stepNo}` |
| `POST` | `/simulation-attempts/{attemptId}/steps/{stepNo}/answers` |
| `POST` | `/simulation-attempts/{attemptId}/complete` |

Start request: `{ "resume":true }`.

Step response:

```json
{
  "attemptId": 7,
  "progress": { "current":2, "answered":1, "total":5 },
  "step": {
    "stepNo":2,
    "screenId":"SIM-02-S02",
    "type":"MULTI_SELECT",
    "title":"필요 서류 확인",
    "prompt":"필요한 서류를 모두 고르세요.",
    "payload": { "choices":[] }
  }
}
```

Answer request: `{ "answer":{ "choiceIds":["resident","income"] } }`. Completion requires all steps to have correct answers and awards XP/badge only once.

## 10. Daily Connect

| Method | Path |
| --- | --- |
| `GET` | `/daily-connect/articles?category=ECONOMY&bookmarkedOnly=false&cursor=` |
| `GET` | `/daily-connect/articles/{articleId}` |
| `PUT` | `/daily-connect/articles/{articleId}/bookmark` |
| `POST` | `/daily-connect/quizzes/{quizId}/answers` |
| `GET` | `/terms/{termId}` |

Article cards include `term`, `youtubeUrl`, `youtubeVideoId`, `thumbnailUrl`, `bookmarked`, and `quizCompleted`. If `youtubeVideoId` exists, the backend provides `thumbnailUrl` as `https://img.youtube.com/vi/{youtubeVideoId}/hqdefault.jpg`. Bookmarks and quiz answers are user-specific DB rows.

Article card example:

```json
{
  "id": "daily_short_stock_market",
  "categoryCode": "ECONOMY",
  "title": "증시",
  "term": "증시",
  "subtitle": "오늘의 경제 용어",
  "summary": [
    "증시 개념을 짧은 영상으로 학습합니다.",
    "카드 썸네일을 누르면 유튜브 Shorts 영상으로 연결할 수 있습니다.",
    "시청 후 퀴즈로 핵심 용어를 확인합니다."
  ],
  "youtubeUrl": "https://www.youtube.com/shorts/ab_8TU7-qtY",
  "youtubeVideoId": "ab_8TU7-qtY",
  "thumbnailUrl": "https://img.youtube.com/vi/ab_8TU7-qtY/hqdefault.jpg",
  "sourceName": "Econo-up Shorts",
  "publishedAt": "2026-06-23T00:00:00Z",
  "bookmarked": false,
  "quizCompleted": false
}
```

Seeded Shorts content:

| Term | YouTube Shorts |
| --- | --- |
| 증시 | `https://www.youtube.com/shorts/ab_8TU7-qtY` |
| 코스피 | `https://www.youtube.com/shorts/rsSh_HcnlA4` |
| 레버리지 | `https://www.youtube.com/shorts/wBNMvj2NTSc` |
| 기금액 | `https://www.youtube.com/shorts/e2Ce54WER54` |
| 양도세 중과 | `https://www.youtube.com/shorts/pG6jw93zPpg` |
| 장기채 | `https://www.youtube.com/shorts/Si2YS5dAPmQ` |
| S&P 500 | `https://www.youtube.com/shorts/KX7uOEiPnN4` |
| 헤지펀드 | `https://www.youtube.com/shorts/a4lMWzaTEUQ` |
| K자 양극화 | `https://www.youtube.com/shorts/-51dpSOcMpo` |
| 분리과세 | `https://www.youtube.com/shorts/TFmHlnb4eAM` |
| MOU | `https://www.youtube.com/shorts/AKF0kxAeevk` |
| 중기자산배분 | `https://www.youtube.com/shorts/Qw3OUyktx6c` |

Quiz request:

```json
{ "answer": { "choiceIds":["choice_a"] } }
```

Live article crawling/curation is an operational feed dependency. The repository includes real DB-backed seed content so the API remains testable without an external key.

## 11. Characters and My Page

| Method | Path |
| --- | --- |
| `GET` | `/my-page/summary` |
| `GET` | `/characters/categories/{categoryCode}` |
| `PUT` | `/characters/{characterId}/equip` |

Each curriculum category has levels 1/2/3 unlocked at category XP `0/500/1500`. Equip validates ownership and stores `users.equipped_character_id`. My Page returns the equipped character, league preview, streak, and real study calendar.

## 12. Social and pokes

| Method | Path | Request |
| --- | --- | --- |
| `GET` | `/friends` | Also claims pending poke rewards |
| `GET` | `/friends/search?nickname=예진` | At least 2 chars |
| `POST` | `/friend-requests` | `{ "receiverId":2 }` |
| `POST` | `/friend-requests/{requestId}/accept` | Receiver only |
| `POST` | `/friend-requests/{requestId}/reject` | Receiver only |
| `DELETE` | `/friends/{friendId}` | Either friend |
| `GET` | `/social/feed` | Weekly friend XP ranking |
| `POST` | `/friends/{friendId}/pokes` | Once per friend/day |

Poke success gives the sender one bill immediately. The receiver's one-bill reward is stored as pending and claimed on the next friend-list access.

## 13. League

| Method | Path |
| --- | --- |
| `GET` | `/leagues/me` |
| `GET` | `/leagues/{leagueId}/ranking` |
| `GET` | `/leagues/results/latest` |

Weekly XP is summed from `study_days`, Monday through Sunday in KST. Rankings are split by tier and limited to 20 users. Top 3 are promoted, bottom 3 are demoted when the group has at least 6 users. Previous-week result finalization is idempotently stored in `league_results`.

## 14. Asynchronous battle

| Method | Path | Request |
| --- | --- | --- |
| `GET` | `/battles/summary` | Win/draw/loss/open count |
| `POST` | `/battles/random-matches` | Creates waiting battle or joins oldest waiting battle |
| `POST` | `/battles/{battleId}/attempts` | Participant only |
| `POST` | `/battle-attempts/{attemptId}/answers` | `{ "questionId":101, "answer":{"choiceIds":["a"]} }` |
| `POST` | `/battle-attempts/{attemptId}/complete` | All 10 questions required |
| `GET` | `/battles/{battleId}/result` | Scores, winner, reactions |
| `POST` | `/battles/{battleId}/reactions` | `{ "type":"GOOD_GAME" }`, once per sender |
| `POST` | `/battles/friend-invites` | `{ "friendId":2 }` |
| `POST` | `/battles/friend-invites/{inviteId}/accept` | Invite receiver only |
| `POST` | `/battles/friend-invites/{inviteId}/reject` | Invite receiver only |
| `GET` | `/battles/history` | Latest 30 |

Both players receive the same server-selected question IDs. Answers are independently stored in `battle_answers`; the battle finishes after both attempts complete. A winner receives three crowns.

## 15. Settings and app info

| Method | Path |
| --- | --- |
| `GET` | `/settings/notifications` |
| `PUT` | `/settings/notifications` |
| `GET` | `/app-info` |

Notification request:

```json
{
  "reviewReminder": { "enabled":true, "time":"07:30" },
  "studyReminder": { "enabled":true, "time":"21:00" },
  "goldenTicket": { "enabled":true },
  "poke": { "enabled":true },
  "league": { "enabled":true }
}
```

`/app-info` exposes `termsUrl`, `privacyUrl`, and `supportEmail` from environment variables. Actual push delivery needs FCM/APNs credentials and a device-token table/provider job.

## 16. Final screen-to-API matrix

Status values: `완료` means server logic and persistence exist. `외부 설정` means backend boundary exists but a provider key, public URL, or operating feed is required.

| Screen ID | Backend data/action | API | Status |
| --- | --- | --- | --- |
| EC-0001 | Splash routing | token + `/onboarding/status` | 완료 |
| EC-0002 | Social login and terms | `/auth/google/login`, `/auth/social/login` | Google 완료; Kakao/Apple 외부 설정 |
| EC-0002-B | Profile/nickname | `/users/nickname-availability`, `/onboarding/profile` | 완료 |
| EC-0003 | Interests | `/onboarding/interests` | 완료 |
| EC-0004 | Goal | `/onboarding/goal` | 완료 |
| EC-0005 | Study style | `/onboarding/study-style` | 완료 |
| EC-0006 | Failure reason | `/onboarding/failure-reason` | 완료 |
| EC-0006-B | Level-test intro/skip | `/level-tests`, `/level-tests/skip` | 완료 |
| EC-0007 | Level-test questions | `/level-tests/{id}/answers` | 완료 |
| EC-0008 | Level-test result | `/level-tests/{id}/complete` | 완료 |
| EC-1001, EC-1001-NEW | Home | `/home` | 완료 |
| EC-1001-P01~P03 | Heart/bill/unlock popups | `/wallet`, `/wallet/hearts/*`, `/wallet/unlocks/*` | 완료; real store purchase 외부 설정 |
| EC-1011 | Daily review | `/reviews/today`, `/reviews/{id}/answers` | 완료 |
| EC-1012 | Review complete | `/reviews/{id}/complete` | 완료 |
| EC-1021 | Streak | `/progress/streak`, `/wallet/streak/revive` | 완료 |
| EC-1031 | Golden ticket | `/golden-tickets/current`, `/golden-tickets/{id}/activate` | 완료 |
| EC-2001 | Categories | `/curriculum/categories` | 완료 |
| EC-2011~EC-2015 | Category roadmaps | `/curriculum/categories/{code}/roadmap` | 완료 |
| EC-2021 | Stage map | `/curriculum/units/{unitId}/stages/{stageId}/map` | 완료 |
| EC-2031~EC-2038 | Learning/question/feedback/exit | `/learning/sessions/*`, `/learning/attempts/*` | 완료 |
| EC-2041 | Learning completion | `/learning/attempts/{id}/complete` | 완료 |
| EC-2051~EC-2058 | First simulation | `/simulations`, `/simulation-attempts/*` | 완료 |
| SIM-02-S01~S05 | Year-end tax quest | `/simulations`, `/simulation-attempts/*` | 완료 |
| SIM-03-S01~S04 | First credit-card quest | same | 완료 |
| SIM-04-S01~S06 | Youth-account quest | same | 완료 |
| SIM-05-S01~S06 | ISA/ETF quest | same | 완료 |
| SIM-06-S01~S06 | Overseas-stock quest | same | 완료 |
| SIM-07 | IRP quest | same | 완료 |
| SIM-08-S01~S05 | Insurance-claim quest | same | 완료 |
| SIM-09-S01~S05 | Credit-score quest | same | 완료 |
| SIM-10-S01~S06 | Income-tax quest | same | 완료 |
| SIM-11-S01~S06 | Fraud-response quest | same | 완료 |
| EC-3001 | News feed/filter/bookmark | `/daily-connect/articles`, bookmark API | 완료; live feed 외부 설정 |
| EC-3011 | News detail/quiz | article, quiz, term APIs | 완료; source content external |
| EC-4001 | League ranking | `/leagues/me`, ranking API | 완료 |
| EC-4002 | League result | `/leagues/results/latest` | 완료 |
| EC-4011 | Social feed/poke | `/social/feed`, poke API | 완료 |
| EC-4012 | Poke received/reward | `/friends` pending reward claim | 완료 |
| EC-4021 | Battle main | `/battles/summary` | 완료 |
| EC-4022-A~C | Random matching/wait/result wait | `/battles/random-matches`, result API | 완료 |
| EC-4023-B | Battle quiz | battle attempt/answer APIs | 완료 |
| EC-4024-NEW | Battle result/reaction | result/reaction APIs | 완료 |
| EC-4025 | Friend battle invite | `/battles/friend-invites` | 완료; Kakao sharing is frontend SDK |
| EC-4026 | Battle history | `/battles/history` | 완료 |
| EC-5001-NEW | My Page | `/my-page/summary`, `/users/me` | 완료 |
| EC-5011-NEW | Characters | character list/equip APIs | 완료 |
| EC-5021 | Learning records/report | `/progress/learning-records`, competency report | 완료 |
| EC-5041~EC-5043 | Bill/heart/revive purchase | `/wallet/*` | internal currency 완료; store receipt 외부 설정 |
| EC-5061 | Settings menu | profile/settings/app APIs | 완료 |
| EC-5062 | Notifications | `/settings/notifications` | preference 저장 완료; delivery 외부 설정 |
| EC-5063 | Account | `/users/me`, `DELETE /users/me` | 완료 |
| EC-5064 | Friends | friend/search/request APIs | 완료 |
| EC-5065 | Login account | `/users/me` | 완료 |
| EC-5066 | App info/terms/support | `/app-info` | API 완료; public URLs 외부 설정 |

## 17. Database tables

### 17.1 Curriculum and learning

| Table | Purpose / important columns |
| --- | --- |
| `categories`, `units`, `stages`, `sessions` | Curriculum hierarchy imported from XLSX |
| `questions` | `payload_json` for frontend, `answer_json` for server grading, explanation/highlight |
| `learning_attempts` | Session attempt state, completion time, XP only |
| `learning_answers` | Unique attempt/question answer JSON, correctness, client/server submitted time |
| `user_category_progress` | Category XP, score, level |
| `level_tests`, `level_test_answers` | Test question IDs, state, answer/correctness |
| `review_sets`, `review_items`, `review_answers` | Daily review selection and user answers |
| `study_days` | KST daily XP, study minutes, completed sessions |

### 17.2 Content and simulations

| Table | Purpose |
| --- | --- |
| `daily_articles`, `terms` | News/term content |
| `article_bookmarks`, `daily_quiz_answers` | User-specific news state |
| `simulations`, `simulation_steps` | 11 quest definitions and JSON interaction data |
| `simulation_attempts`, `simulation_step_answers` | User progress and submitted answers |

### 17.3 Economy, social, and competition

| Table | Purpose |
| --- | --- |
| `users` | Profile, progress summary, bills/hearts/passes/tickets, character/tier/crowns/settings |
| `purchases` | Bill ledger including grant, spend, poke reward |
| `content_entitlements` | Timed/permanent user content access |
| `golden_tickets` | Issue, expiry, activation, preview session IDs |
| `friendships`, `pokes` | Friend request state and once-per-day poke rewards |
| `league_results` | Finalized weekly rank, tier change, crowns |
| `battles`, `battle_attempts`, `battle_answers`, `battle_reactions` | Async battle state, same question set, scores, reactions |

Hibernate uses `ddl-auto=update` in local/runtime configuration and `create-drop` in H2 tests. Production deployment should replace automatic update with reviewed migrations before public launch.

## 18. Required deployment settings

| Environment variable / provider | Required for |
| --- | --- |
| `DB_PASSWORD` | MySQL connection; no password is committed |
| `JWT_SECRET` | Production tokens; replace local default |
| `GOOGLE_CLIENT_ID` | Google audience validation |
| Kakao/Apple credentials | Those login buttons |
| Store server credentials | App Store/Play receipt validation, refund and settlement |
| `TERMS_URL`, `PRIVACY_URL`, `SUPPORT_EMAIL` | EC-5066 and login terms links |
| FCM/APNs credentials and device tokens | Actual reminder/poke/league notifications |
| News source/API/editorial feed | Continuously updated production news and YouTube metadata |

The current backend is runnable without these provider values for local development, but provider-dependent flows are not considered production-complete until the values and operating policies are supplied.
