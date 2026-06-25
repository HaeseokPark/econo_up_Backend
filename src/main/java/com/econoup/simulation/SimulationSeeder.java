package com.econoup.simulation;

import com.econoup.curriculum.QuestionInteractionService;
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SimulationSeeder {
    private final SimulationRepository simulationRepository;
    private final SimulationStepRepository stepRepository;
    private final QuestionInteractionService json;

    public SimulationSeeder(SimulationRepository simulationRepository, SimulationStepRepository stepRepository, QuestionInteractionService json) {
        this.simulationRepository = simulationRepository;
        this.stepRepository = stepRepository;
        this.json = json;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        seedSimulation("sim_subscription", "REAL_ESTATE", "청약 당첨 후 60일의 기록", "🏠", 1L, 200, "내 집 마련 완료",
                multi("EC-2053", "서류 준비", "계약에 필요한 서류를 모두 고르세요.", List.of("A", "B", "C", "E", "F"),
                        "A", "주민등록등본", "B", "가족관계증명서", "C", "근로소득 원천징수영수증", "D", "여권 사본", "E", "건강보험료 납부확인서", "F", "인감증명서"),
                multi("EC-2054", "계약서 확인", "확인이 필요한 위험 조항을 모두 고르세요.", List.of("B", "C"),
                        "A", "계약금 10%", "B", "입주일 변경 가능", "C", "계약 해제 시 계약금 전액 몰수", "D", "발코니 확장 포함"),
                number("EC-2055", "계약금 납부", "4억 2,000만원의 10% 계약금은 몇 만원인가요?", 4200),
                single("EC-2056", "중도금 대출", "일반적인 분양 중도금에 더 편리한 방식은?", "A", "A", "집단 대출", "B", "개별 대출"),
                number("EC-2057", "잔금과 등기", "예상 취득세를 만원 단위로 입력하세요.", 630));

        seedSimulation("sim_year_end_tax", "TAX", "연말정산 시즌", "🧾", 2L, 200, "절세 마스터",
                single("SIM-02-S02", "간소화 서비스", "연말정산 간소화 서비스는 어디서 이용할까요?", "A", "A", "홈택스 앱 또는 홈페이지", "B", "회사 인트라넷", "C", "은행 앱"),
                multi("SIM-02-S03", "공제 항목 분류", "세액공제 항목을 고르세요.", List.of("B", "C", "D"), "A", "신용카드 사용금액", "B", "연금저축·IRP", "C", "보장성 보험료", "D", "의료비"),
                info("SIM-02-S04", "환급 결과 확인", "기납부세액 45만원, 결정세액 28만원이면 환급액은 17만원입니다.", "결정세액이 더 크면 추징될 수 있습니다."));

        seedSimulation("sim_first_credit_card", "SAVING", "첫 신용카드", "💳", 3L, 150, "첫 신용관리",
                single("SIM-03-S02", "신용점수 조회", "무료로 신용점수를 확인할 수 있는 방법은?", "A", "A", "나이스지키미·카카오페이·토스", "B", "신청 후 자동 확인", "C", "확인하지 않아도 됨"),
                single("SIM-03-S03", "카드 선택 기준", "첫 카드의 우선 기준은?", "A", "A", "연회비 없음과 소비처 혜택", "B", "한도가 가장 높은 카드", "C", "광고를 많이 본 카드", "D", "적립률만 높은 카드"),
                ordering("SIM-03-S04", "자동이체 설정", "발급 직후 순서를 맞추세요.", "A", "B", "C", "D"));

        seedSimulation("sim_youth_account", "SAVING", "청년도약계좌", "🏦", 4L, 250, "청년정책러",
                multi("SIM-04-S02", "자격 확인", "가입 요건을 모두 고르세요.", List.of("A", "B", "C", "E"), "A", "만 19~34세", "B", "개인소득 7,500만원 이하", "C", "가구소득 중위 180% 이하", "D", "대기업 재직 필수", "E", "금융소득종합과세 비대상"),
                info("SIM-04-S03", "은행 비교", "기본금리와 내가 충족할 수 있는 우대금리 조건을 비교하세요.", "우대 조건을 못 채우면 기본금리만 적용됩니다."),
                ordering("SIM-04-S04", "신청 절차", "신청 순서를 맞추세요.", "A", "B", "C", "D", "E"),
                single("SIM-04-S05", "납입 설정", "자동이체 날짜로 적절한 시점은?", "A", "A", "급여 입금일 다음 날", "B", "매월 1일", "C", "생각날 때", "D", "만기에 한꺼번에"));

        seedSimulation("sim_isa_etf", "STOCK", "ISA + ETF 첫 투자", "📊", 5L, 250, "첫 투자자",
                single("SIM-05-S02", "ISA 유형 선택", "ETF 직접 매매 목적에 맞는 ISA 유형은?", "A", "A", "중개형 ISA", "B", "일반형 ISA", "C", "서민형 ISA", "D", "ISA는 ETF에 부적합"),
                info("SIM-05-S03", "납입과 ETF 매수", "ISA 이체 → 계좌 선택 → ETF 검색 → 주문 → 체결 순서로 진행합니다.", "연간 납입 한도는 2,000만원입니다."),
                single("SIM-05-S04", "비과세 한도", "일반형 ISA 비과세 한도는?", "A", "A", "연 200만원", "B", "전액 비과세", "C", "일반계좌와 동일", "D", "배당은 무조건 과세"),
                ordering("SIM-05-S05", "적립 루틴", "월간 ETF 적립 루틴을 맞추세요.", "A", "B", "C", "D"));

        seedSimulation("sim_overseas_stock", "STOCK", "해외 주식 첫 거래", "🌐", 6L, 300, "글로벌 투자자",
                info("SIM-06-S02", "환전", "증권사 앱에서 환율과 수수료 우대를 확인하고 원화를 달러로 환전합니다.", "환손익도 투자 결과에 포함됩니다."),
                single("SIM-06-S03", "거래 시간과 주문", "미국 정규장의 한국 시간은?", "A", "A", "동절기 23:30~06:00, 하절기 22:30~05:00", "B", "09:00~15:30", "C", "24시간", "D", "주말"),
                info("SIM-06-S04", "배당과 세금", "미국 배당은 한미 조세협약에 따라 보통 15% 원천징수됩니다.", "증권사 앱의 배당 내역을 확인하세요."),
                single("SIM-06-S05", "양도소득세", "해외주식 양도차익 기본공제는?", "A", "A", "연 250만원", "B", "자동 신고", "C", "매도 시 원천징수", "D", "공제 없음"));

        seedSimulation("sim_irp", "SAVING", "IRP 세액공제", "🏛️", 7L, 200, "연금 설계자",
                info("SIM-07-S02", "세액공제 계산", "연금저축과 IRP 합산 세액공제 한도는 최대 900만원입니다.", "총급여에 따라 공제율이 달라집니다."),
                ordering("SIM-07-S03", "개설과 납입", "IRP 개설과 납입 순서를 맞추세요.", "A", "B", "C", "D", "E"),
                single("SIM-07-S04", "운용 제한", "IRP의 위험자산 최대 비중은?", "A", "A", "70%", "B", "100%", "C", "0%", "D", "50%"));

        seedSimulation("sim_insurance_claim", "SAVING", "실손보험 청구", "🏥", 8L, 150, "보험 청구왕",
                multi("SIM-08-S02", "서류 수집", "실손보험 청구 필요 서류를 고르세요.", List.of("A", "B", "C", "E"), "A", "진료비 영수증", "B", "진료비 세부내역서", "C", "의사 소견서", "D", "주민등록등본", "E", "처방전"),
                single("SIM-08-S03", "청구 방법", "가장 빠른 일반적인 청구 방법은?", "A", "A", "보험사 앱 사진 업로드", "B", "설계사 전화", "C", "우편", "D", "병원 자동 청구"),
                info("SIM-08-S04", "보장 범위", "4세대 실손은 급여 80%, 비급여 70% 보장을 기본 구조로 합니다.", "가입 시기와 약관에 따라 다를 수 있습니다."));

        seedSimulation("sim_credit_score", "SAVING", "신용점수 6개월 플랜", "📈", 9L, 200, "신용관리러",
                info("SIM-09-S02", "점수 조회", "NICE와 KCB 점수는 무료 조회할 수 있고 조회 자체는 점수에 영향을 주지 않습니다.", "두 기관 점수가 다를 수 있습니다."),
                multi("SIM-09-S03", "좋은 행동과 나쁜 행동", "신용점수에 좋은 행동을 고르세요.", List.of("A", "D"), "A", "카드값 전액 자동납부", "B", "통신비 3개월 연체", "C", "단기간 카드 3개 발급", "D", "한도 30% 이내 사용"),
                ordering("SIM-09-S04", "6개월 실행 계획", "실행 순서를 맞추세요.", "A", "B", "C", "D", "E"));

        seedSimulation("sim_income_tax", "TAX", "종합소득세 신고", "📑", 10L, 250, "세금 마스터",
                multi("SIM-10-S02", "신고 대상", "종합소득세 신고 대상을 고르세요.", List.of("A", "B", "C", "E"), "A", "프리랜서 소득", "B", "유튜브·블로그 수익", "C", "두 곳 이상 근로소득", "D", "다른 소득 없는 연말정산 완료 직장인", "E", "해외주식 양도차익"),
                info("SIM-10-S03", "소득 유형", "프리랜서·자영업 소득은 보통 사업소득으로 종합소득세 신고 대상입니다.", "3.3% 원천징수 후 환급이 생길 수 있습니다."),
                ordering("SIM-10-S04", "홈택스 신고", "신고 순서를 맞추세요.", "A", "B", "C", "D", "E"),
                multi("SIM-10-S05", "필요경비", "업무 관련 필요경비를 고르세요.", List.of("A", "B", "D"), "A", "업무용 노트북", "B", "업무 교재·강의", "C", "집 전체 월세", "D", "증빙 있는 업무 미팅 식사", "E", "개인 취미 용품"));

        seedSimulation("sim_fraud_response", "ECONOMY", "금융 사기 대응", "🚨", 11L, 200, "사기 방지왕",
                multi("SIM-11-S02", "사기 유형 판별", "즉시 끊어야 하는 신호를 고르세요.", List.of("A", "B", "C", "D"), "A", "금감원 사칭 계좌 연루", "B", "검사 사칭 이체 요구", "C", "대환대출 선상환 요구", "D", "인증번호 요구", "E", "은행 공식 앱 로그인 알림"),
                ordering("SIM-11-S03", "즉시 대응", "피해 직후 대응 순서를 맞추세요.", "A", "B", "C", "D", "E"),
                info("SIM-11-S04", "피해구제", "거래 은행에 피해구제를 신청하고 사실확인서와 신분증을 제출합니다.", "환급은 사기 계좌 잔액 범위에서 진행됩니다."),
                multi("SIM-11-S05", "예방 수칙", "올바른 예방 행동을 고르세요.", List.of("A", "B", "C", "E"), "A", "모르는 링크 클릭 금지", "B", "공식 스토어 앱 설치", "C", "인증번호 미공유", "D", "전화로 계좌번호 제공", "E", "출처 불명 앱 설치 중단"));
    }

    private void seedSimulation(String id, String category, String title, String icon, Long unlockStageId,
                                int rewardXp, String badgeName, StepSeed... steps) {
        SimulationEntity simulation = simulationRepository.findById(id).orElseGet(SimulationEntity::new);
        simulation.id = id;
        simulation.categoryCode = category;
        simulation.title = title;
        simulation.description = title + " 시뮬레이션을 단계별로 진행합니다.";
        simulation.icon = icon;
        simulation.unlockStageId = unlockStageId;
        simulation.totalSteps = steps.length;
        simulation.rewardXp = rewardXp;
        simulation.badgeName = badgeName;
        simulation.active = true;
        simulationRepository.save(simulation);
        if (!stepRepository.findBySimulation_IdOrderByStepNoAsc(id).isEmpty()) return;
        for (int i = 0; i < steps.length; i++) {
            StepSeed seed = steps[i];
            SimulationStepEntity step = new SimulationStepEntity();
            step.simulation = simulation;
            step.stepNo = i + 1;
            step.screenId = seed.screenId;
            step.type = seed.type;
            step.title = seed.title;
            step.prompt = seed.prompt;
            step.payloadJson = json.writeJson(seed.payload);
            step.answerJson = json.writeJson(seed.answer);
            step.explanation = seed.explanation;
            stepRepository.save(step);
        }
    }

    private StepSeed single(String screenId, String title, String prompt, String correct, String... pairs) {
        return choiceStep(screenId, "SINGLE_CHOICE", title, prompt, List.of(correct), pairs);
    }

    private StepSeed multi(String screenId, String title, String prompt, List<String> correct, String... pairs) {
        return choiceStep(screenId, "MULTI_SELECT", title, prompt, correct, pairs);
    }

    private StepSeed choiceStep(String screenId, String type, String title, String prompt, List<String> correct, String... pairs) {
        List<Map<String, String>> choices = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) choices.add(Map.of("id", pairs[i], "text", pairs[i + 1]));
        return new StepSeed(screenId, type, title, prompt, Map.of("choices", choices), Map.of("choiceIds", correct), "선택 내용을 다시 확인해보세요.");
    }

    private StepSeed ordering(String screenId, String title, String prompt, String... ids) {
        List<Map<String, String>> items = new ArrayList<>();
        for (String id : ids) items.add(Map.of("id", id, "text", "단계 " + id));
        return new StepSeed(screenId, "ORDERING", title, prompt, Map.of("items", items), Map.of("orderedItemIds", List.of(ids)), "올바른 순서를 확인해보세요.");
    }

    private StepSeed number(String screenId, String title, String prompt, int answer) {
        return new StepSeed(screenId, "NUMBER_INPUT", title, prompt, Map.of("unit", "만원"), Map.of("numberValue", answer), "계산식을 다시 확인해보세요.");
    }

    private StepSeed info(String screenId, String title, String prompt, String tip) {
        return new StepSeed(screenId, "INFO", title, prompt, Map.of("tip", tip), Map.of("acknowledged", true), tip);
    }

    private record StepSeed(String screenId, String type, String title, String prompt,
                            Map<String, Object> payload, Map<String, Object> answer, String explanation) {}
}
