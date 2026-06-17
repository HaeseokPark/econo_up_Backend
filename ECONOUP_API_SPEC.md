# Econo-up API 명세서 초안

## 0. 문서 정보

| 항목 | 내용 |
| --- | --- |
| 서비스 | 이코노업(Econo-up) |
| 문서 목적 | 모바일 앱 및 백엔드 개발 착수를 위한 API 계약 초안 |
| 기준 산출물 | 기획안, `경제 듀오링고.pdf` 와이어프레임 v1.0~v1.1 |
| 작성 범위 | 전체 서비스 API 초안 + MVP/Post-MVP 구분 |
| 상태 | Draft |

## 1. 명세 전제

### 1.1 핵심 제품 루프

1. 사용자는 온보딩과 레벨테스트로 시작 카테고리를 추천받는다.
2. 카테고리 > 유닛 > 스테이지 > 세션 순서로 짧은 학습을 진행한다.
3. 학습 완료 결과는 XP, 스트릭, 카테고리 성장도, 복습 큐에 반영된다.
4. 복습 퀴즈로 이전 학습 개념을 회상한다.
5. 시뮬레이션과 데일리 커넥트로 배운 개념을 실전 맥락에 연결한다.

### 1.2 MVP 범위

| 구분 | 기능 |
| --- | --- |
| MVP | 소셜 로그인, 온보딩, 레벨테스트, 홈, 학습 로드맵, 학습 세션, 정오답 피드백, 복습 퀴즈, 스트릭, 학습 기록, 시뮬레이션 1종, 데일리 커넥트 기본 조회, 마이페이지 기본 |
| BM 실험 | 하트, 지폐, 부활권, 유료 카테고리 해금, 골든 티켓 |
| Post-MVP | 친구, 찌르기, 리그, 비동기 배틀, 친구 배틀, 배틀 전적, 공유, 고급 알림 |

> 와이어프레임에는 BM과 소셜 화면이 상세히 포함되어 있다. 본 명세는 전체 API를 정의하되, 구현 우선순위는 MVP 태그를 기준으로 판단한다.

### 1.2.1 2026-06-17 배포 구현 상태

| 구분 | 구현 상태 |
| --- | --- |
| 핵심 MVP | 온보딩, 홈, 커리큘럼, 로드맵, 스테이지 맵, 학습 세션, 답안 제출/채점, 세션 완료, 복습, 지폐 재화, 마이페이지/설정 기본 API 구현 |
| 데이터 구조 | `questions.payload_json`, `questions.answer_json`, `learning_answers`, `review_sets`, `review_items`, `review_answers`, `purchases` 반영 |
| 배포 안정화용 API | 뉴스/용어, 시뮬레이션, 캐릭터, 골든 티켓, 친구/소셜, 리그, 배틀 API는 404 방지를 위한 MVP/placeholder 응답 제공 |
| 후속 고도화 | 실제 결제/영수증 검증/세금 정산, 뉴스 운영 데이터 연동, 시뮬레이션 분기 로직, 리그/배틀 매칭 로직, 소셜 그래프 저장은 후속 범위 |

### 1.3 기술 가정

| 항목 | 가정 |
| --- | --- |
| API 스타일 | REST JSON API |
| Base URL | `/api/v1` |
| 인증 | 소셜 로그인 후 Bearer Access Token |
| 시간 | ISO 8601 UTC 저장, 응답에는 timezone-aware 문자열 사용 |
| 페이지네이션 | cursor 기반 |
| 파일/이미지 | CDN URL 문자열 반환 |
| 결제 | 앱스토어/플레이스토어 영수증 검증 API 연동 |
| 알림 | 앱 서버가 푸시 토큰과 알림 설정을 저장 |

## 2. 공통 규약

### 2.1 Header

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
X-Client-Platform: ios | android | web
X-Client-Version: 1.0.0
X-Timezone: Asia/Seoul
```

### 2.2 공통 응답 포맷

#### 성공

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "req_01J...",
    "serverTime": "2026-05-22T03:00:00Z"
  }
}
```

#### 실패

```json
{
  "success": false,
  "error": {
    "code": "LEARNING_SESSION_LOCKED",
    "message": "이전 세션을 완료해야 시작할 수 있습니다.",
    "details": {
      "requiredSessionId": "sess_rate_01"
    }
  },
  "meta": {
    "requestId": "req_01J...",
    "serverTime": "2026-05-22T03:00:00Z"
  }
}
```

### 2.3 공통 상태 코드

| HTTP | 의미 |
| --- | --- |
| `200` | 조회/수정 성공 |
| `201` | 생성 성공 |
| `204` | 삭제 성공 |
| `400` | 요청 형식 오류 |
| `401` | 인증 필요 또는 토큰 만료 |
| `403` | 권한 없음/잠금 콘텐츠 |
| `404` | 리소스 없음 |
| `409` | 중복 요청/상태 충돌 |
| `422` | 비즈니스 검증 실패 |
| `429` | 요청 제한 초과 |
| `500` | 서버 오류 |

### 2.4 공통 에러 코드

| 코드 | 설명 |
| --- | --- |
| `AUTH_TOKEN_INVALID` | 토큰이 유효하지 않음 |
| `AUTH_SOCIAL_LOGIN_FAILED` | 소셜 로그인 검증 실패 |
| `USER_NOT_FOUND` | 사용자 없음 |
| `NICKNAME_DUPLICATED` | 닉네임 중복 |
| `CONTENT_NOT_FOUND` | 콘텐츠 없음 |
| `CONTENT_LOCKED` | 해금되지 않은 콘텐츠 |
| `PURCHASE_REQUIRED` | 결제가 필요한 콘텐츠 |
| `HEART_NOT_ENOUGH` | 하트 부족 |
| `BILL_NOT_ENOUGH` | 지폐 부족 |
| `REVIEW_NOT_AVAILABLE` | 현재 복습 문항 없음 |
| `ALREADY_COMPLETED` | 이미 완료한 작업 |
| `INVALID_ANSWER` | 제출 답안 형식 오류 |
| `RATE_LIMITED` | 요청 제한 초과 |

### 2.5 공통 Enum

```ts
type CategoryCode = "ECONOMY" | "SAVING" | "STOCK" | "REAL_ESTATE" | "TAX";
type ContentAccessType = "FREE" | "PAID_LOCKED" | "PASS_OWNED" | "RENTED" | "PERMANENT_OWNED" | "TICKET_ACCESS";
type SessionType = "THEORY" | "TERM_MATCH" | "DRILL" | "CONNECTION" | "DATA";
type QuestionType = "OX" | "SINGLE_CHOICE" | "MULTI_SELECT" | "MATCHING" | "ORDERING" | "CHART_POINT" | "NUMBER_INPUT";
type LearningStatus = "LOCKED" | "AVAILABLE" | "IN_PROGRESS" | "COMPLETED";
type PurchaseProductType = "BILL_PACK" | "HEART_REFILL" | "HEART_UNLIMITED_24H" | "STREAK_REVIVE" | "CATEGORY_PASS" | "UNIT_RENTAL" | "UNIT_PERMANENT";
```

## 3. 핵심 데이터 모델

### 3.1 User

```json
{
  "id": "usr_01J...",
  "nickname": "경제왕",
  "gender": "MALE",
  "age": 27,
  "avatarUrl": null,
  "onboardingCompleted": true,
  "levelTestCompleted": true,
  "createdAt": "2026-05-22T03:00:00Z"
}
```

### 3.2 UserProgressSummary

```json
{
  "xp": 760,
  "streakDays": 14,
  "todayStudyCompleted": false,
  "heart": {
    "current": 3,
    "max": 3,
    "nextRefillAt": "2026-05-22T06:00:00Z",
    "unlimitedUntil": null
  },
  "billBalance": 5,
  "categoryProgress": [
    {
      "categoryCode": "ECONOMY",
      "progressPercent": 60,
      "level": 2,
      "xp": 420
    }
  ]
}
```

### 3.3 Curriculum

```json
{
  "category": {
    "code": "ECONOMY",
    "name": "경제 상식",
    "subtitle": "세상 돌아가는 흐름을 읽는 자가 돈을 지킨다",
    "accessType": "FREE"
  },
  "units": [
    {
      "id": "unit_rate",
      "title": "금리",
      "subtitle": "돈에도 몸값이 있다",
      "status": "COMPLETED",
      "progressPercent": 100
    }
  ]
}
```

### 3.4 LearningQuestion

> 현재 구현 기준: 프런트에는 `questions.payload_json` 기반 렌더링 데이터만 내려간다. `questions.answer_json`은 서버 채점 전용이며 문제 조회/세션 시작 응답에는 포함하지 않는다.

```json
{
  "id": 4,
  "type": "SINGLE_CHOICE",
  "prompt": "금융통화위원회는 1년에 몇 회 정기 회의를 열까요?",
  "context": {
    "categoryCode": "ECONOMY",
    "unitTitle": "금리",
    "stageTitle": "기준금리 기초"
  },
  "choices": [
    { "id": "A", "text": "4회" },
    { "id": "B", "text": "8회" },
    { "id": "C", "text": "12회" }
  ],
  "answerPolicy": {
    "revealImmediately": true
  },
  "resource": {
    "type": "TEXT",
    "text": "현행 기준 약 6주마다 한 번씩 연 8회 개최."
  },
  "payload": {
    "interactionType": "객관식(A·B·C)"
  }
}
```

#### 주요 필드

| Field | 설명 |
| --- | --- |
| `id` | `questions.id` 숫자 ID |
| `type` | `THEORY_CARD`, `OX`, `SINGLE_CHOICE`, `TEXT_INPUT`, `NUMBER_INPUT`, `MATCHING`, `CLASSIFICATION`, `ORDERING`, `DIRECTION_SELECT`, `CHART_POINT` 등 |
| `prompt` | 문제/설명 본문 |
| `context` | 카테고리/유닛/스테이지 표시용 정보 |
| `choices` | 선택지 기반 문제일 때만 포함 |
| `resource` | 보조 자료가 있을 때만 포함. 현재는 xlsx 원문 리소스를 `{ type: "TEXT", text }` 형태로 저장 |
| `payload` | 프런트 UI 분기 보조 데이터. 현재 `interactionType`은 xlsx의 인터랙션 방식 원문 |
| `answerPolicy` | 현재 MVP는 즉시 피드백 기준으로 `revealImmediately: true` |

### 3.5 AnswerFeedback

> 답안 제출 후에만 반환된다. `correctAnswer`는 `questions.answer_json` 기반으로 서버가 내려주는 채점 결과용 데이터다.

```json
{
  "correct": true,
  "correctAnswer": {
    "choiceIds": ["B"]
  },
  "explanation": "약 6주(1.5개월)마다 한 번씩, 연 8회 개최.",
  "highlightText": "FOMC는 보통 연 8회 개최",
  "reward": {
    "xpGained": 10,
    "heartConsumed": 0
  }
}
```

## 4. 인증 및 계정 API

### 4.1 소셜 로그인

| 항목 | 내용 |
| --- | --- |
| Scope | MVP |
| Method | `POST` |
| Path | `/auth/social/login` |
| 화면 | EC-0002 |
| 설명 | 카카오/Apple/Google 토큰을 검증하고 앱 토큰을 발급한다. |

#### Request

```json
{
  "provider": "KAKAO",
  "providerToken": "social_token",
  "termsAgreed": true,
  "device": {
    "platform": "ios",
    "deviceId": "device_abc",
    "pushToken": "fcm_or_apns_token"
  }
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "accessToken": "access_token",
    "refreshToken": "refresh_token",
    "isNewUser": true,
    "nextScreen": "ONBOARDING_PROFILE"
  }
}
```

### 4.2 토큰 갱신

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/auth/token/refresh` | MVP |

```json
{
  "refreshToken": "refresh_token"
}
```

### 4.3 로그아웃

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/auth/logout` | MVP | EC-5065 |

### 4.4 회원 탈퇴

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `DELETE` | `/users/me` | MVP | EC-5065 |

## 5. 온보딩 API

### 5.1 기본 정보 저장

| 항목 | 내용 |
| --- | --- |
| Scope | MVP |
| Method | `PUT` |
| Path | `/onboarding/profile` |
| 화면 | EC-0002-B |

#### Request

```json
{
  "nickname": "경제왕",
  "gender": "MALE",
  "age": 27
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "profileSaved": true,
    "nextStep": "INTERESTS"
  }
}
```

### 5.2 닉네임 중복 검사

| Method | Path | Scope |
| --- | --- | --- |
| `GET` | `/users/nickname-availability?nickname={nickname}` | MVP |

```json
{
  "success": true,
  "data": {
    "nickname": "경제왕",
    "available": false
  }
}
```

### 5.3 관심 분야 저장

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/onboarding/interests` | MVP | EC-0003 |

```json
{
  "categoryCodes": ["ECONOMY", "SAVING", "STOCK"]
}
```

### 5.4 학습 목적 저장

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/onboarding/goal` | MVP Optional | EC-0004 |

```json
{
  "goal": "LOSS_PREVENTION"
}
```

### 5.5 학습 스타일 저장

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/onboarding/study-style` | MVP Optional | EC-0005 |

```json
{
  "frequency": "DAILY",
  "depth": "NORMAL",
  "sessionVolume": "ONE_TO_TWO"
}
```

### 5.6 공부 실패 이유 저장

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/onboarding/failure-reason` | Research Optional | EC-0006 |

```json
{
  "reason": "BORING_AND_HARD",
  "skipped": false
}
```

### 5.7 온보딩 상태 조회

| Method | Path | Scope |
| --- | --- | --- |
| `GET` | `/onboarding/status` | MVP |

```json
{
  "success": true,
  "data": {
    "profileCompleted": true,
    "interestsCompleted": true,
    "goalCompleted": false,
    "studyStyleCompleted": false,
    "levelTestCompleted": false,
    "nextStep": "LEVEL_TEST_INTRO"
  }
}
```

## 6. 레벨테스트 API

### 6.1 레벨테스트 생성

| 항목 | 내용 |
| --- | --- |
| Scope | MVP |
| Method | `POST` |
| Path | `/level-tests` |
| 화면 | EC-0006-B |

#### Request

```json
{
  "questionCount": 10
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "testId": "lt_01J...",
    "estimatedMinutes": 3,
    "questionCount": 10,
    "firstQuestion": {
      "id": "lt_q_001",
      "type": "SINGLE_CHOICE",
      "prompt": "기준금리가 오르면 대출 이자는 일반적으로 어떻게 될까요?",
      "choices": [
        { "id": "A", "text": "오른다" },
        { "id": "B", "text": "내린다" }
      ]
    }
  }
}
```

### 6.2 레벨테스트 답안 제출

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/level-tests/{testId}/answers` | MVP | EC-0007 |

```json
{
  "questionId": "lt_q_001",
  "answer": {
    "choiceIds": ["A"]
  }
}
```

```json
{
  "success": true,
  "data": {
    "accepted": true,
    "nextQuestion": {
      "id": "lt_q_002",
      "type": "OX",
      "prompt": "예금과 적금은 같은 상품이다."
    },
    "progress": {
      "answered": 1,
      "total": 10
    }
  }
}
```

### 6.3 레벨테스트 완료

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/level-tests/{testId}/complete` | MVP | EC-0008 |

```json
{
  "success": true,
  "data": {
    "resultType": "FOUNDATION_REQUIRED",
    "resultTitle": "기초 탄탄 필요형",
    "recommendedCategoryCode": "ECONOMY",
    "recommendedUnitId": "unit_rate",
    "recommendedStageId": "stage_rate_basic"
  }
}
```

### 6.4 레벨테스트 건너뛰기

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/level-tests/skip` | MVP |

## 7. 홈 API

### 7.1 홈 대시보드 조회

| 항목 | 내용 |
| --- | --- |
| Scope | MVP |
| Method | `GET` |
| Path | `/home` |
| 화면 | EC-1001, EC-1001-NEW |

#### Response

```json
{
  "success": true,
  "data": {
    "user": {
      "nickname": "경제왕",
      "isNewLearner": false
    },
    "summary": {
      "streakDays": 14,
      "heartCurrent": 3,
      "heartMax": 3,
      "billBalance": 5
    },
    "today": {
      "review": {
        "available": true,
        "completed": false,
        "questionCount": 5,
        "ctaPath": "/reviews/today"
      },
      "dailyConnect": {
        "available": true,
        "completed": false,
        "headlineId": "news_fed_hold_20260428",
        "headline": "미 연준, 기준금리 동결 결정"
      }
    },
    "continueLearning": [
      {
        "categoryCode": "ECONOMY",
        "categoryName": "경제 상식",
        "unitId": "unit_rate",
        "unitTitle": "금리",
        "stageId": "stage_rate_market",
        "progressPercent": 60
      }
    ],
    "recommendedSimulation": {
      "simulationId": "sim_subscription_contract",
      "title": "청약 당첨! 그 이후",
      "unlocked": true
    },
    "goldenTicket": null,
    "leaguePreview": null
  }
}
```

### 7.2 홈 피드 관심 분야 수정

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/users/me/home-interests` | MVP | EC-1001-P03, EC-5063 |

```json
{
  "categoryCodesInOrder": ["ECONOMY", "SAVING"]
}
```

## 8. 커리큘럼 API

### 8.1 카테고리 목록 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/curriculum/categories` | MVP | EC-2001 |

```json
{
  "success": true,
  "data": {
    "categories": [
      {
        "code": "ECONOMY",
        "name": "경제 상식",
        "description": "금리·물가·환율",
        "accessType": "FREE",
        "progressPercent": 60
      },
      {
        "code": "STOCK",
        "name": "주식",
        "description": "주식·ETF·투자심리",
        "accessType": "PAID_LOCKED",
        "progressPercent": 0
      }
    ]
  }
}
```

### 8.2 카테고리 로드맵 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/curriculum/categories/{categoryCode}/roadmap` | MVP | EC-2011~EC-2015 |

#### Response 핵심 필드

```json
{
  "success": true,
  "data": {
    "category": {
      "code": "SAVING",
      "name": "저축",
      "subtitle": "시드머니를 만드는 갓생 루틴",
      "characterTitle": "새싹 저축러",
      "accessType": "FREE"
    },
    "roadmap": {
      "completedUnitCount": 1,
      "totalUnitCount": 3,
      "progressPercent": 33
    },
    "units": [
      {
        "id": "unit_cashflow",
        "title": "현금관리",
        "subtitle": "새는 돈 수사대",
        "status": "COMPLETED",
        "stagePreview": ["지출 분석", "통장 시스템", "카드 전략"]
      },
      {
        "id": "unit_bank",
        "title": "은행 정복",
        "subtitle": "이자의 마법",
        "status": "IN_PROGRESS",
        "stagePreview": ["예금과 적금", "단리와 복리", "예금자 보호"]
      }
    ]
  }
}
```

### 8.3 유닛 스테이지 맵 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/curriculum/units/{unitId}/stages/{stageId}/map` | MVP | EC-2021 |

```json
{
  "success": true,
  "data": {
    "unit": {
      "id": "unit_rate",
      "title": "금리"
    },
    "stage": {
      "id": "stage_rate_market",
      "title": "금리와 시장",
      "completedSessionCount": 1,
      "totalSessionCount": 5,
      "status": "IN_PROGRESS"
    },
    "sessions": [
      {
        "id": "sess_rate_market_theory",
        "type": "THEORY",
        "title": "금리와 시장 개요",
        "status": "COMPLETED"
      },
      {
        "id": "sess_rate_market_drill_01",
        "type": "DRILL",
        "title": "유리 vs 불리",
        "status": "AVAILABLE"
      }
    ],
    "simulationCta": {
      "unlocked": false,
      "unlockCondition": "STAGE_COMPLETION"
    }
  }
}
```

## 9. 학습 세션 API

### 9.1 세션 시작

| 항목 | 내용 |
| --- | --- |
| Scope | MVP |
| Method | `POST` |
| Path | `/learning/sessions/{sessionId}/attempts` |
| 화면 | EC-2031~EC-2035 |

#### Request

```json
{
  "resume": true
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "attemptId": 101,
    "session": {
      "id": 4,
      "code": "E-U1-S1-04",
      "type": "DRILL",
      "title": "객관식(A·B·C)",
      "categoryCode": "ECONOMY",
      "unitId": 1,
      "unitTitle": "금리",
      "stageId": 1,
      "stageTitle": "기준금리 기초"
    },
    "progress": {
      "current": 1,
      "total": 1
    },
    "question": {
      "id": 4,
      "type": "SINGLE_CHOICE",
      "prompt": "금융통화위원회는 1년에 몇 회 정기 회의를 열까요?",
      "context": {
        "categoryCode": "ECONOMY",
        "unitTitle": "금리",
        "stageTitle": "기준금리 기초"
      },
      "choices": [
        { "id": "A", "text": "4회" },
        { "id": "B", "text": "8회" },
        { "id": "C", "text": "12회" }
      ],
      "answerPolicy": {
        "revealImmediately": true
      },
      "resource": {
        "type": "TEXT",
        "text": "현행 기준 약 6주마다 한 번씩 연 8회 개최. ※ 회의 횟수는 한국은행 정책에 따라 변경될 수 있으며, 최신 일정은 한국은행 공식 홈페이지에서 확인하세요."
      },
      "payload": {
        "interactionType": "객관식(A·B·C)"
      }
    }
  }
}
```

> `question` 응답에는 정답이 포함되지 않는다. 프런트는 `type`, `choices`, `resource`, `payload.interactionType`을 이용해 화면을 구성하고, 답안 제출 API로 사용자 답만 보낸다.

### 9.2 세션 답안 제출

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/learning/attempts/{attemptId}/answers` | MVP | EC-2032~EC-2037 |

#### Single Choice Request

```json
{
  "questionId": 4,
  "answer": {
    "choiceIds": ["B"]
  },
  "clientAnsweredAt": "2026-05-22T03:10:00Z"
}
```

#### OX Request

```json
{
  "questionId": 2,
  "answer": {
    "choiceIds": ["X", "O"]
  }
}
```

> 현재 xlsx 원본에는 `OX 버튼 (2문항)`처럼 한 세션 안에 복합 OX가 들어간 항목이 있다. MVP 구현에서는 `choiceIds` 배열 순서로 제출한다.

#### Ordering Request

```json
{
  "questionId": 10,
  "answer": {
    "orderedItemIds": ["A", "C", "B", "D", "E"]
  }
}
```

#### Number Input Request

```json
{
  "questionId": 12,
  "answer": {
    "numberValue": 8
  }
}
```

#### Text Input Request

```json
{
  "questionId": 3,
  "answer": {
    "answerText": "BOK 금융통화위원회 7"
  }
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "feedback": {
      "correct": true,
      "correctAnswer": {
        "choiceIds": ["B"]
      },
      "explanation": "약 6주(1.5개월)마다 한 번씩, 연 8회 개최.",
      "highlightText": "FOMC는 보통 연 8회 개최",
      "reward": {
        "xpGained": 10,
        "heartConsumed": 0
      }
    },
    "progress": {
      "current": 1,
      "answered": 1,
      "total": 1
    },
    "nextQuestion": null
  }
}
```

#### 구현 메모

| 항목 | 현재 백엔드 기준 |
| --- | --- |
| 제출 저장 | `learning_answers.submitted_answer_json`에 `answer` 객체를 JSON 문자열로 저장 |
| 채점 기준 | `questions.answer_json`과 제출 `answer` 비교 |
| 중복 제출 | 같은 `attemptId` + `questionId`는 기존 `learning_answers` 행을 갱신 |
| 보상 | 정답 제출 시 즉시 `xpGained: 10`, 오답 시 `heartConsumed: 1` |
| 세션 XP | `learning_attempts.xp_gained`는 해당 attempt의 정답 수 기반으로 갱신 |

### 9.3 세션 이탈 저장

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/learning/attempts/{attemptId}/exit` | MVP | EC-2038 |

```json
{
  "reason": "USER_EXIT",
  "lastQuestionId": "q_rate_bond_01"
}
```

### 9.4 세션 완료

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/learning/attempts/{attemptId}/complete` | MVP | EC-2041 |

```json
{
  "success": true,
  "data": {
    "sessionCompleted": true,
    "stageCompleted": false,
    "xpGained": 50,
    "growth": {
      "categoryCode": "ECONOMY",
      "beforePercent": 54,
      "afterPercent": 62,
      "deltaPercent": 8
    },
    "next": {
      "nextSessionId": "sess_rate_market_connection",
      "nextStageId": null,
      "simulationUnlocked": false
    }
  }
}
```

### 9.5 스테이지 완료 결과 조회

| Method | Path | Scope |
| --- | --- | --- |
| `GET` | `/learning/stages/{stageId}/completion-summary` | MVP |

## 10. 복습 API

> 현재 구현 기준: 복습은 기존 학습 오답을 우선으로 하고, 부족하면 최근 학습 문제와 초기 문제를 섞어 오늘의 복습 세트를 만든다. 복잡한 추천 알고리즘은 MVP 범위에서 제외한다.

### 10.1 오늘 복습 세트 조회

| 항목 | 내용 |
| --- | --- |
| Scope | MVP |
| Method | `GET` |
| Path | `/reviews/today` |
| 화면 | EC-1011 |

```json
{
  "success": true,
  "data": {
    "reviewSetId": 12,
    "status": "IN_PROGRESS",
    "progress": {
      "current": 1,
      "answered": 0,
      "total": 5
    },
    "question": {
      "id": 4,
      "type": "SINGLE_CHOICE",
      "prompt": "금융통화위원회는 1년에 몇 회 정기 회의를 열까요?",
      "choices": [
        { "id": "A", "text": "4회" },
        { "id": "B", "text": "8회" },
        { "id": "C", "text": "12회" }
      ],
      "source": {
        "categoryCode": "ECONOMY",
        "stageTitle": "기준금리 기초"
      },
      "resource": {
        "type": "TEXT",
        "text": "현행 기준 약 6주마다 한 번씩 연 8회 개최."
      }
    }
  }
}
```

### 10.2 복습 답안 제출

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/reviews/{reviewSetId}/answers` | MVP | EC-1011 |

#### Request

```json
{
  "questionId": 4,
  "answer": {
    "choiceIds": ["B"]
  }
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "feedback": {
      "correct": true,
      "correctAnswer": {
        "rawText": "B. 8회",
        "choiceIds": ["B"]
      },
      "explanation": "약 6주(1.5개월)마다 한 번씩, 연 8회 개최.",
      "highlightText": "FOMC는 보통 연 8회 개최",
      "reward": {
        "xpGained": 5,
        "heartConsumed": 0
      }
    },
    "progress": {
      "current": 2,
      "answered": 1,
      "total": 5
    },
    "nextQuestion": {
      "id": 5,
      "type": "NUMBER_INPUT",
      "prompt": "화면의 한국 기준금리 역사 그래프를 보고 두 가지를 완성하세요."
    }
  }
}
```

### 10.3 복습 완료

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/reviews/{reviewSetId}/complete` | MVP | EC-1012 |

```json
{
  "success": true,
  "data": {
    "correctCount": 4,
    "totalCount": 5,
    "accuracyPercent": 80,
    "xpGained": 20,
    "streak": {
      "beforeDays": 14,
      "afterDays": 14
    }
  }
}
```

#### 구현 메모

| 항목 | 현재 백엔드 기준 |
| --- | --- |
| 복습 세트 | `review_sets`에 사용자별 오늘 날짜로 1개 생성 |
| 복습 문항 | `review_items`에 최대 5개 저장 |
| 문항 선정 | 오답 `learning_answers` 우선, 부족하면 최근 학습 attempt의 문제, 그래도 부족하면 초기 문제 |
| 답안 저장 | `review_answers.submitted_answer_json`에 저장 |
| 보상 | 복습 정답당 5 XP |

## 11. 스트릭 및 학습 기록 API

### 11.1 스트릭 상세 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/progress/streak` | MVP | EC-1021 |

```json
{
  "success": true,
  "data": {
    "currentStreakDays": 14,
    "month": "2026-05",
    "studyDates": [
      "2026-05-20",
      "2026-05-21",
      "2026-05-22"
    ],
    "streakBroken": false,
    "reviveTicketBalance": 0
  }
}
```

### 11.2 전체 학습 기록 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/progress/learning-records?period=month&date=2026-05` | MVP | EC-5021 |

```json
{
  "success": true,
  "data": {
    "summary": {
      "completedStageCount": 42,
      "streakDays": 14,
      "totalXp": 1280
    },
    "calendar": [
      {
        "date": "2026-05-22",
        "studyMinutes": 12,
        "intensity": 2
      }
    ],
    "weeklyStudyMinutes": [
      { "weekday": "MON", "minutes": 5 },
      { "weekday": "TUE", "minutes": 10 }
    ]
  }
}
```

### 11.3 경제 역량 리포트 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/progress/competency-report` | MVP Recommended | 신규 리포트 화면 |

```json
{
  "success": true,
  "data": {
    "radar": [
      { "categoryCode": "ECONOMY", "score": 62, "percentile": 48 },
      { "categoryCode": "SAVING", "score": 54, "percentile": 41 },
      { "categoryCode": "STOCK", "score": 10, "percentile": null }
    ],
    "recentGrowth": [
      {
        "categoryCode": "ECONOMY",
        "label": "금리와 시장 관계 이해",
        "deltaScore": 8
      }
    ],
    "weakConcepts": [
      {
        "conceptId": "concept_tax_deduction",
        "label": "세액공제와 소득공제 차이"
      }
    ],
    "recommendedStage": {
      "stageId": "stage_saving_compound",
      "title": "단리와 복리"
    }
  }
}
```

## 12. 시뮬레이션 API

### 12.1 시뮬레이션 목록 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/simulations` | MVP | EC-2051 |

```json
{
  "success": true,
  "data": {
    "simulations": [
      {
        "id": "sim_subscription_after_win",
        "title": "청약 당첨! 그 이후",
        "unlockCondition": "stage_realestate_subscription_02",
        "status": "AVAILABLE"
      },
      {
        "id": "sim_first_loan",
        "title": "첫 대출 준비",
        "status": "LOCKED"
      }
    ]
  }
}
```

### 12.2 시뮬레이션 시작

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/simulations/{simulationId}/attempts` | MVP | EC-2052 |

```json
{
  "success": true,
  "data": {
    "attemptId": "simatt_01J...",
    "opening": {
      "title": "청약 당첨 후 60일의 기록",
      "scenario": {
        "assetName": "경기도 하남시 감일 푸르지오 59㎡",
        "price": 420000000,
        "wonAt": "2026-04-20",
        "contractDeadline": "2026-04-28"
      },
      "steps": [
        { "stepNo": 1, "title": "서류 준비" },
        { "stepNo": 2, "title": "계약서 확인" }
      ]
    }
  }
}
```

### 12.3 시뮬레이션 단계 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/simulation-attempts/{attemptId}/steps/{stepNo}` | MVP | EC-2053~EC-2057 |

```json
{
  "success": true,
  "data": {
    "step": {
      "stepNo": 1,
      "title": "서류 준비",
      "situation": "청약 당첨 후 7일 이내 계약에 필요한 서류를 준비해야 합니다.",
      "mission": {
        "type": "MULTI_SELECT",
        "prompt": "필요한 서류를 모두 고르세요.",
        "choices": [
          { "id": "resident", "text": "주민등록등본" },
          { "id": "passport", "text": "여권 사본" }
        ]
      }
    }
  }
}
```

### 12.4 시뮬레이션 단계 답안 제출

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/simulation-attempts/{attemptId}/steps/{stepNo}/answers` | MVP | EC-2053~EC-2057 |

```json
{
  "answer": {
    "choiceIds": ["resident", "family", "income", "seal_certificate"]
  }
}
```

```json
{
  "success": true,
  "data": {
    "accepted": true,
    "correct": true,
    "feedback": {
      "summary": "계약 서류 준비가 완료되었습니다.",
      "warnings": [
        "신분증은 사본이 아니라 원본 지참이 필요합니다."
      ],
      "decisionCriteria": [
        "청약 자격 증빙",
        "소득 증빙",
        "계약서 서명 준비"
      ]
    },
    "nextStepNo": 2
  }
}
```

### 12.5 시뮬레이션 완료

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/simulation-attempts/{attemptId}/complete` | MVP | EC-2058 |

```json
{
  "success": true,
  "data": {
    "xpGained": 200,
    "timeline": [
      { "label": "당첨 확인", "day": "D+0" },
      { "label": "서류 준비", "day": "D+7" }
    ],
    "keyConcepts": [
      {
        "conceptId": "concept_contract_deposit",
        "label": "계약금 = 분양가 × 10%"
      }
    ],
    "reviewLinks": [
      {
        "conceptId": "concept_contract_deposit",
        "stageId": "stage_realestate_contract"
      }
    ]
  }
}
```

## 13. 데일리 커넥트 API

### 13.1 뉴스 피드 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/daily-connect/articles?category={categoryCode}&cursor={cursor}` | MVP | EC-3001 |

```json
{
  "success": true,
  "data": {
    "headline": {
      "id": "article_fed_hold_20260428",
      "title": "미 연준, 기준금리 동결 결정",
      "summary": "시장 예상에 부합, 연내 인하 기대 유지",
      "categoryCode": "ECONOMY",
      "conceptTags": ["금리"],
      "quizCompleted": true,
      "publishedAt": "2026-04-28T00:00:00Z"
    },
    "items": [
      {
        "id": "article_fx_20260406",
        "title": "환율 급등, 원/달러 1,400원 돌파",
        "categoryCode": "ECONOMY",
        "bookmarked": false,
        "publishedAt": "2026-04-06T00:00:00Z"
      }
    ],
    "nextCursor": "cursor_abc"
  }
}
```

### 13.2 뉴스 상세 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/daily-connect/articles/{articleId}` | MVP | EC-3011 |

```json
{
  "success": true,
  "data": {
    "article": {
      "id": "article_fed_hold_20260428",
      "title": "미 연준, 기준금리 동결 결정",
      "subtitle": "시장 예상에 부합, 연내 인하 기대 유지",
      "publishedAt": "2026-04-28T00:00:00Z",
      "sourceName": "Example News",
      "sourceUrl": "https://example.com/article",
      "aiSummary": [
        "연준은 기준금리를 현행 수준에서 동결했습니다.",
        "인플레이션 둔화를 확인하나 추가 근거가 필요합니다.",
        "시장은 연내 인하 가능성을 유지 중입니다."
      ]
    },
    "terms": [
      {
        "termId": "term_base_rate",
        "name": "기준금리",
        "shortDefinition": "중앙은행이 결정하는 시중금리의 기준"
      }
    ],
    "relatedLearning": {
      "stageId": "stage_rate_basic",
      "label": "경제 상식 > Unit 1. 금리"
    },
    "understandingQuiz": {
      "quizId": "dcq_fed_hold_01",
      "available": true,
      "completed": false
    }
  }
}
```

### 13.3 용어 상세 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/terms/{termId}` | MVP | EC-3011-P01 |

### 13.4 북마크 토글

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/daily-connect/articles/{articleId}/bookmark` | MVP | EC-3001, EC-3011 |

```json
{
  "bookmarked": true
}
```

### 13.5 데일리 커넥트 이해 퀴즈 제출

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/daily-connect/quizzes/{quizId}/answers` | MVP Recommended |

## 14. 캐릭터 및 마이페이지 API

### 14.1 내 프로필 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/users/me` | MVP | EC-5001-NEW |

### 14.2 마이페이지 요약 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/my-page/summary` | MVP | EC-5001-NEW |

```json
{
  "success": true,
  "data": {
    "profile": {
      "nickname": "경제왕",
      "equippedCharacterId": "char_saving_seedling"
    },
    "streakDays": 14,
    "leaguePreview": null,
    "characters": [
      {
        "id": "char_saving_seedling",
        "categoryCode": "SAVING",
        "name": "새싹 저축러",
        "level": 1,
        "owned": true,
        "equipped": true
      }
    ],
    "learningCalendarPreview": [
      {
        "date": "2026-05-22",
        "intensity": 2
      }
    ]
  }
}
```

### 14.3 카테고리 캐릭터 성장 단계 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/characters/categories/{categoryCode}` | MVP Optional | EC-5011-NEW |

### 14.4 캐릭터 장착

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/characters/{characterId}/equip` | MVP Optional | EC-5011-NEW |

## 15. 재화 및 BM API

> 6/15 MVP 구현 기준: 실제 앱스토어/구글플레이 결제, 세금/정산, 스토어 영수증 검증은 제외한다. 현재는 앱 내 지폐(`BILL`) 잔액 조회, 개발용/이벤트성 지급, 차감 API만 제공한다.

### 15.1 지갑 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/wallet` | MVP | EC-1001, EC-5041 |

```json
{
  "success": true,
  "data": {
    "billBalance": 15,
    "currency": "BILL"
  }
}
```

### 15.2 지폐 지급

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/wallet/bills/grant` | MVP Dev/Event |

> 실제 결제 연동 전까지 개발용 지급, 이벤트성 지급, 운영자 지급 등에 사용한다.

#### Request

```json
{
  "amount": 10,
  "memo": "MVP signup bonus"
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "wallet": {
      "billBalance": 15,
      "currency": "BILL"
    },
    "transaction": {
      "type": "DEV_GRANT",
      "amount": 10
    }
  }
}
```

### 15.3 지폐 차감

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/wallet/bills/spend` | MVP |

#### Request

```json
{
  "amount": 3,
  "memo": "review_retry"
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "wallet": {
      "billBalance": 12,
      "currency": "BILL"
    },
    "transaction": {
      "type": "SPEND",
      "amount": 3
    }
  }
}
```

#### Error

```json
{
  "success": false,
  "error": {
    "code": "BILL_NOT_ENOUGH",
    "message": "Bill balance is not enough."
  }
}
```

### 15.4 후속 결제 연동 범위

아래 항목은 6/15 MVP에서 제외하고 후속 배포에서 별도 구현한다.

| 기능 | 후속 구현 내용 |
| --- | --- |
| 상품 목록 | `/store/products` |
| 스토어 결제 검증 | `/store/purchases/verify`, iOS/Android 영수증 검증 |
| 환불/취소 처리 | 결제 취소 시 지폐/권한 회수 |
| 세금/정산 | 부가세/VAT, 스토어 수수료, 사업자 정산 기준 확인 |
| 유료 해금 | 카테고리 패스, 골든 티켓, 콘텐츠 권한 부여 |

## 16. 골든 티켓 API

### 16.1 현재 골든 티켓 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/golden-tickets/current` | BM | EC-1031 |

```json
{
  "success": true,
  "data": {
    "ticket": {
      "id": "ticket_01J...",
      "unitId": "unit_etf_basic",
      "unitTitle": "ETF 기초",
      "expiresAt": "2026-05-22T15:00:00Z",
      "previewStages": [
        "주식 시장의 구조",
        "ETF란 무엇인가",
        "ETF 선택 기준"
      ]
    }
  }
}
```

### 16.2 티켓 사용 시작

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/golden-tickets/{ticketId}/activate` | BM |

## 17. 설정 및 알림 API

### 17.1 알림 설정 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/settings/notifications` | MVP | EC-5062 |

### 17.2 알림 설정 수정

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `PUT` | `/settings/notifications` | MVP | EC-5062 |

```json
{
  "reviewReminder": {
    "enabled": true,
    "time": "07:30"
  },
  "goldenTicket": {
    "enabled": true
  },
  "poke": {
    "enabled": false
  },
  "league": {
    "enabled": false
  },
  "studyReminder": {
    "enabled": true,
    "time": "21:00"
  }
}
```

### 17.3 앱 정보 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/app-info` | MVP | EC-5066 |

## 18. 소셜 API

### 18.1 친구 목록 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/friends` | Post-MVP | EC-5064 |

### 18.2 친구 검색

| Method | Path | Scope |
| --- | --- | --- |
| `GET` | `/friends/search?nickname={nickname}` | Post-MVP |

### 18.3 친구 요청 생성

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/friend-requests` | Post-MVP |

### 18.4 친구 요청 수락/거절

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/friend-requests/{requestId}/accept` | Post-MVP |
| `POST` | `/friend-requests/{requestId}/reject` | Post-MVP |

### 18.5 친구 삭제

| Method | Path | Scope |
| --- | --- | --- |
| `DELETE` | `/friends/{friendId}` | Post-MVP |

### 18.6 소셜 피드 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/social/feed` | Post-MVP | EC-4011 |

### 18.7 찌르기

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/friends/{friendId}/pokes` | Post-MVP | EC-4011, EC-4012 |

```json
{
  "success": true,
  "data": {
    "poked": true,
    "reward": {
      "senderBill": 1,
      "receiverBillPending": 1
    }
  }
}
```

## 19. 리그 API

### 19.1 내 리그 현황 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/leagues/me` | Post-MVP | EC-4001 |

### 19.2 리그 랭킹 조회

| Method | Path | Scope |
| --- | --- | --- |
| `GET` | `/leagues/{leagueId}/ranking` | Post-MVP |

### 19.3 리그 결과 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/leagues/results/latest` | Post-MVP | EC-4002 |

## 20. 배틀 API

### 20.1 배틀 메인 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/battles/summary` | Post-MVP | EC-4021 |

### 20.2 랜덤 매칭 요청

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/battles/random-matches` | Post-MVP | EC-4022-A, EC-4022-C |

```json
{
  "mode": "ASYNC_TIER_MATCH"
}
```

```json
{
  "success": true,
  "data": {
    "matchStatus": "OPPONENT_ALREADY_READY",
    "battleId": "battle_01J...",
    "opponent": {
      "nickname": "머니킹",
      "tier": "BRONZE",
      "recentWinRate": 62
    },
    "questionCount": 10
  }
}
```

### 20.3 배틀 퀴즈 시작

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/battles/{battleId}/attempts` | Post-MVP | EC-4023-B |

### 20.4 배틀 답안 제출

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/battle-attempts/{attemptId}/answers` | Post-MVP | EC-4023-B |

### 20.5 배틀 시도 완료

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/battle-attempts/{attemptId}/complete` | Post-MVP | EC-4022-B, EC-4024-NEW |

### 20.6 배틀 결과 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/battles/{battleId}/result` | Post-MVP | EC-4024-NEW |

### 20.7 배틀 반응 전송

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/battles/{battleId}/reactions` | Post-MVP | EC-4024-NEW |

```json
{
  "reaction": "RESPECT"
}
```

### 20.8 친구 배틀 초대 생성

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `POST` | `/battles/friend-invites` | Post-MVP | EC-4025 |

### 20.9 친구 배틀 초대 수락/거절

| Method | Path | Scope |
| --- | --- | --- |
| `POST` | `/battles/friend-invites/{inviteId}/accept` | Post-MVP |
| `POST` | `/battles/friend-invites/{inviteId}/reject` | Post-MVP |

### 20.10 배틀 전적 조회

| Method | Path | Scope | 화면 |
| --- | --- | --- | --- |
| `GET` | `/battles/history?cursor={cursor}` | Post-MVP | EC-4026 |

## 21. 화면별 API 매핑

### 21.1 온보딩

| 화면 ID | 화면 | 주요 API |
| --- | --- | --- |
| EC-0001 | 스플래시 | `/onboarding/status`, 토큰 상태 확인 |
| EC-0002 | 회원가입 | `POST /auth/social/login` |
| EC-0002-B | 기본 정보 입력 | `GET /users/nickname-availability`, `PUT /onboarding/profile` |
| EC-0003 | 관심 분야 | `PUT /onboarding/interests` |
| EC-0004 | 학습 목적 | `PUT /onboarding/goal` |
| EC-0005 | 학습 스타일 | `PUT /onboarding/study-style` |
| EC-0006 | 실패 이유 | `PUT /onboarding/failure-reason` |
| EC-0006-B | 레벨테스트 안내 | `POST /level-tests`, `POST /level-tests/skip` |
| EC-0007 | 레벨테스트 | `POST /level-tests/{testId}/answers` |
| EC-0008 | 테스트 결과 | `POST /level-tests/{testId}/complete` |

### 21.2 홈/복습

| 화면 ID | 화면 | 주요 API |
| --- | --- | --- |
| EC-1001 | 홈 메인 | `GET /home` |
| EC-1001-NEW | 신규 홈 | `GET /home` |
| EC-1011 | 복습 퀴즈 | `GET /reviews/today`, `POST /reviews/{reviewSetId}/answers` |
| EC-1012 | 복습 완료 | `POST /reviews/{reviewSetId}/complete` |
| EC-1021 | 스트릭 상세 | `GET /progress/streak` |
| EC-1031 | 골든 티켓 | `GET /golden-tickets/current` |

### 21.3 학습/시뮬레이션

| 화면 ID | 화면 | 주요 API |
| --- | --- | --- |
| EC-2001 | 카테고리 목록 | `GET /curriculum/categories` |
| EC-2011~2015 | 카테고리 로드맵 | `GET /curriculum/categories/{categoryCode}/roadmap` |
| EC-2021 | 스테이지 맵 | `GET /curriculum/units/{unitId}/stages/{stageId}/map` |
| EC-2031~2035 | 학습 세션 | `POST /learning/sessions/{sessionId}/attempts`, `POST /learning/attempts/{attemptId}/answers` |
| EC-2036~2037 | 정오답 피드백 | 답안 제출 응답의 `feedback` |
| EC-2038 | 이탈 확인 | `POST /learning/attempts/{attemptId}/exit` |
| EC-2041 | 스테이지 완료 | `POST /learning/attempts/{attemptId}/complete` |
| EC-2051 | 시뮬레이션 목록 | `GET /simulations` |
| EC-2052 | 시뮬레이션 오프닝 | `POST /simulations/{simulationId}/attempts` |
| EC-2053~2057 | 시뮬레이션 단계 | `GET /simulation-attempts/{attemptId}/steps/{stepNo}`, `POST /simulation-attempts/{attemptId}/steps/{stepNo}/answers` |
| EC-2058 | 시뮬레이션 완료 | `POST /simulation-attempts/{attemptId}/complete` |

### 21.4 데일리 커넥트

| 화면 ID | 화면 | 주요 API |
| --- | --- | --- |
| EC-3001 | 뉴스 피드 | `GET /daily-connect/articles` |
| EC-3011 | 뉴스 상세 | `GET /daily-connect/articles/{articleId}` |
| EC-3011-P01 | 용어 팝업 | `GET /terms/{termId}` |

### 21.5 마이/설정

| 화면 ID | 화면 | 주요 API |
| --- | --- | --- |
| EC-5001-NEW | 마이페이지 | `GET /my-page/summary` |
| EC-5011-NEW | 캐릭터 단계 | `GET /characters/categories/{categoryCode}`, `PUT /characters/{characterId}/equip` |
| EC-5021 | 학습 기록 | `GET /progress/learning-records` |
| EC-5061 | 설정 | 정적 메뉴 |
| EC-5062 | 알림 설정 | `GET /settings/notifications`, `PUT /settings/notifications` |
| EC-5063 | 관심 분야 | `PUT /users/me/home-interests` |
| EC-5065 | 계정 설정 | `POST /auth/logout`, `DELETE /users/me` |
| EC-5066 | 앱 정보/약관 | `GET /app-info` |

## 22. 백엔드 엔티티 초안

| 엔티티 | 핵심 필드 |
| --- | --- |
| `users` | id, social_provider, social_subject, nickname, gender, age, onboarding_completed |
| `user_interests` | user_id, category_code, priority |
| `user_study_styles` | user_id, frequency, depth, session_volume |
| `level_tests` | id, user_id, status, result_type |
| `level_test_answers` | test_id, question_id, answer_json, correct |
| `categories` | code, name, access_type |
| `units` | id, category_code, sequence, title, subtitle |
| `stages` | id, unit_id, sequence, title |
| `sessions` | id, stage_id, code, sequence, type, title |
| `questions` | id, session_id, sequence, type, prompt, payload_json, answer_json, explanation, highlight_text |
| `learning_attempts` | id, user_id, session_id, status, xp_gained, started_at, completed_at |
| `learning_answers` | id, attempt_id, question_id, submitted_answer_json, correct, answered_at, client_answered_at |
| `review_sets` | id, user_id, local_date, status, xp_gained, created_at, completed_at |
| `review_items` | id, review_set_id, question_id, sequence |
| `review_answers` | id, review_set_id, question_id, submitted_answer_json, correct, answered_at |
| `study_days` | user_id, local_date, study_minutes, xp_gained |
| `user_category_progress` | user_id, category_code, xp, score, level |
| `simulations` | id, category_code, title, unlock_stage_id |
| `simulation_steps` | simulation_id, step_no, payload_json |
| `simulation_attempts` | id, user_id, simulation_id, status |
| `daily_articles` | id, source_url, title, ai_summary_json, published_at |
| `terms` | id, name, definition, related_stage_id |
| `wallets` | MVP는 별도 테이블 없이 `users.bill_balance` 사용 |
| `purchases` | id, user_id, type, amount, status, memo, created_at |
| `content_entitlements` | user_id, target_type, target_id, access_type, expires_at |
| `characters` | id, category_code, name, unlock_rule_json |
| `user_characters` | user_id, character_id, owned, equipped |
| `notification_settings` | user_id, review_enabled, review_time, study_enabled, study_time |
| `friends` | user_id, friend_user_id, status |
| `pokes` | id, sender_id, receiver_id, reward_status |
| `leagues` | id, tier, season_id |
| `battle_matches` | id, mode, user_a_id, user_b_id, status |
| `battle_attempts` | id, battle_id, user_id, answer_status, score |

## 23. 정책 확정 필요 항목

| 항목 | 현재 초안 | 확정 필요 이유 |
| --- | --- | --- |
| 학습 완료 기준 | 세션 문제 전체 진행 완료 | 이론 세션과 문제 세션 기준이 다를 수 있음 |
| 하트 차감 | 스테이지당 최대 1회 | 와이어프레임과 상품 정책 일치 확인 필요 |
| 스트릭 반영 시점 | 학습 또는 복습 완료 시 | 뉴스/시뮬레이션도 스트릭 인정할지 결정 필요 |
| 복습 큐 생성 | 세션 완료 개념 기준 | 오답 우선, 간격 반복 규칙 확정 필요 |
| 레벨테스트 추천 | 카테고리/유닛/스테이지 반환 | 추천 알고리즘 MVP 단순 룰 확정 필요 |
| 카테고리 유료 해금 | 패스/기간 소장/영구 소장 | 유닛 구매와 카테고리 패스의 권한 관계 확정 필요 |
| 뉴스 콘텐츠 | 과거 이슈 우선 | 최신 뉴스 수집/저작권/출처 정책 필요 |
| 시뮬레이션 피드백 | 판단 기준과 결과 제공 | 금융 조언처럼 보이지 않는 문구 정책 필요 |
| 배틀 점수 | 정답 수 + 응답 속도 | 동점 처리와 어뷰징 방지 필요 |
| 찌르기 보상 | 송신자 즉시, 수신자 접속 시 | 일일 제한과 재화 악용 방지 필요 |

## 24. 구현 순서 제안

### Phase 1. 학습 MVP

1. 인증/온보딩
2. 카테고리/로드맵/스테이지맵
3. 학습 세션/답안/완료
4. 복습 퀴즈
5. 홈/마이 학습 기록
6. 시뮬레이션 1종

### Phase 2. 실전 연결과 리포트

1. 데일리 커넥트 피드/상세/용어
2. 데일리 커넥트 이해 퀴즈
3. 경제 역량 리포트
4. 알림 설정

### Phase 3. BM

1. 하트/지폐/상품
2. 콘텐츠 권한과 결제
3. 골든 티켓
4. 부활권

### Phase 4. 소셜

1. 친구/소셜 피드/찌르기
2. 리그
3. 비동기 배틀
4. 친구 배틀

## 25. API 목록 요약

| 도메인 | 엔드포인트 수 |
| --- | --- |
| 인증/계정 | 4 |
| 온보딩 | 7 |
| 레벨테스트 | 4 |
| 홈 | 2 |
| 커리큘럼 | 3 |
| 학습 | 5 |
| 복습 | 3 |
| 진행/리포트 | 3 |
| 시뮬레이션 | 5 |
| 데일리 커넥트 | 5 |
| 마이/캐릭터 | 4 |
| BM/지갑/결제 | 9 |
| 골든 티켓 | 2 |
| 설정 | 3 |
| 소셜 | 7 |
| 리그 | 3 |
| 배틀 | 10 |

## 26. 다음 산출물 권장

1. 이 문서를 기준으로 MVP API만 추린 `OpenAPI 3.1 YAML`
2. 콘텐츠 CMS 명세서
3. 학습 문제 `payload_json` 스키마 상세
4. ERD
5. 화면별 API 호출 시퀀스 다이어그램
