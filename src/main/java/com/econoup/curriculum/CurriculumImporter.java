package com.econoup.curriculum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CurriculumImporter {
    private static final Logger log = LoggerFactory.getLogger(CurriculumImporter.class);
    private static final Pattern UNIT_PATTERN = Pattern.compile("Unit\\s+(\\d+)\\.\\s*(.+?)(?::\\s*(.+))?$");
    private static final Pattern STAGE_PATTERN = Pattern.compile("Stage\\s+(\\d+)\\.\\s*(.+)$");
    private static final Pattern CHOICE_ID = Pattern.compile("(?<![A-Za-z])([A-Z])\\s*[.)]");
    private static final Pattern PLAIN_CHOICE_ID = Pattern.compile("(?<![A-Za-z])([A-Z])(?![A-Za-z])");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d[\\d,]*(?:\\.\\d+)?");
    private static final Pattern BRACKET_CHOICE_SEGMENT = Pattern.compile("\\[([A-Z])\\]\\s*(.+?)(?=\\s*\\[[A-Z]\\]|$)", Pattern.DOTALL);

    private final CategoryRepository categoryRepository;
    private final CurriculumUnitRepository unitRepository;
    private final StageRepository stageRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final boolean importOnStartup;
    private final Resource curriculumResource;

    public CurriculumImporter(
            CategoryRepository categoryRepository,
            CurriculumUnitRepository unitRepository,
            StageRepository stageRepository,
            SessionRepository sessionRepository,
            QuestionRepository questionRepository,
            ObjectMapper objectMapper,
            @Value("${app.curriculum.import-on-startup}") boolean importOnStartup,
            @Value("${app.curriculum.path}") Resource curriculumResource
    ) {
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.stageRepository = stageRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.importOnStartup = importOnStartup;
        this.curriculumResource = curriculumResource;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void importIfNeeded() throws Exception {
        if (!importOnStartup) {
            log.info("Curriculum import skipped: app.curriculum.import-on-startup=false");
            return;
        }

        try (InputStream inputStream = curriculumResource.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            log.info("Curriculum import/sync started: {} sheets found", workbook.getNumberOfSheets());
            importSheet(workbook, 1, new CategoryEntity("ECONOMY", "\uACBD\uC81C \uC0C1\uC2DD", "\uC138\uC0C1 \uB3CC\uC544\uAC00\uB294 \uD750\uB984\uC744 \uC77D\uB294 \uAE30\uBCF8\uAE30", 1));
            importSheet(workbook, 2, new CategoryEntity("SAVING", "\uC800\uCD95", "\uC6D4\uAE09\uC744 \uC9C0\uD0A4\uACE0 \uBD88\uB9AC\uB294 \uCCAB \uB8E8\uD2F4", 2));
            importSheet(workbook, 3, new CategoryEntity("STOCK", "\uC8FC\uC2DD", "\uAE30\uC5C5\uACFC \uC2DC\uC7A5\uC744 \uC77D\uB294 \uD22C\uC790 \uAE30\uCD08", 3));
            log.info("Curriculum import/sync finished: {} sessions and {} questions saved",
                    sessionRepository.count(), questionRepository.count());
        }
    }

    private void importSheet(Workbook workbook, int sheetIndex, CategoryEntity categorySeed) {
        if (workbook.getNumberOfSheets() <= sheetIndex) {
            log.warn("Curriculum sheet skipped: sheet index {} does not exist", sheetIndex);
            return;
        }

        Sheet sheet = workbook.getSheetAt(sheetIndex);
        CategoryEntity category = categoryRepository.findById(categorySeed.code)
                .orElseGet(() -> categoryRepository.save(categorySeed));
        CurriculumUnitEntity currentUnit = null;
        StageEntity currentStage = null;
        int syncedSessionCount = 0;

        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String first = stringCell(row.getCell(0));
            String sessionCode = stringCell(row.getCell(1));

            if (first.startsWith("Unit")) {
                Matcher matcher = UNIT_PATTERN.matcher(first);
                if (matcher.matches()) {
                    int sequence = Integer.parseInt(matcher.group(1));
                    currentUnit = unitRepository.findByCategory_CodeAndSequence(category.code, sequence)
                            .orElseGet(() -> unitRepository.save(new CurriculumUnitEntity(category, sequence, matcher.group(2), matcher.group(3))));
                    currentUnit.title = matcher.group(2);
                    currentUnit.subtitle = matcher.group(3);
                    unitRepository.save(currentUnit);
                }
                continue;
            }

            if (first.startsWith("Stage")) {
                Matcher matcher = STAGE_PATTERN.matcher(first);
                if (matcher.matches() && currentUnit != null) {
                    int sequence = Integer.parseInt(matcher.group(1));
                    CurriculumUnitEntity unit = currentUnit;
                    currentStage = stageRepository.findByUnit_IdAndSequence(unit.id, sequence)
                            .orElseGet(() -> stageRepository.save(new StageEntity(unit, sequence, matcher.group(2))));
                    currentStage.title = matcher.group(2);
                    stageRepository.save(currentStage);
                }
                continue;
            }

            if (currentStage == null || first.isBlank() || sessionCode.isBlank()) continue;

            int sequence = (int) numericCell(row.getCell(0));
            String contentType = stringCell(row.getCell(2));
            String interactionType = stringCell(row.getCell(3));
            String prompt = stringCell(row.getCell(4));
            String choicesText = nullableStringCell(row.getCell(5));
            String resourceText = nullableStringCell(row.getCell(6));
            String answerText = nullableStringCell(row.getCell(7));
            String explanation = nullableStringCell(row.getCell(8));

            ParsedQuestion parsed = normalizeQuestion(interactionType, prompt, choicesText, resourceText, answerText);
            StageEntity stage = currentStage;
            SessionEntity session = sessionRepository.findByCode(sessionCode)
                    .orElseGet(() -> new SessionEntity(stage, sessionCode, sequence, toSessionType(contentType), interactionType));
            session.stage = stage;
            session.sequence = sequence;
            session.type = toSessionType(contentType);
            session.title = interactionType;
            session = sessionRepository.save(session);

            SessionEntity savedSession = session;
            QuestionEntity question = questionRepository.findFirstBySession_IdOrderBySequenceAsc(session.id)
                    .orElseGet(() -> new QuestionEntity(savedSession, 1, parsed.type(), parsed.prompt(), parsed.payloadJson(), parsed.answerJson(), explanation, explanation));
            question.session = savedSession;
            question.sequence = 1;
            question.type = parsed.type();
            question.prompt = parsed.prompt();
            question.payloadJson = parsed.payloadJson();
            question.answerJson = parsed.answerJson();
            question.explanation = explanation;
            question.highlightText = explanation;
            questionRepository.save(question);
            syncedSessionCount++;
        }

        log.info("Curriculum sheet synced: sheet='{}', category={}, sessions={}",
                sheet.getSheetName(), category.code, syncedSessionCount);
    }

    private ParsedQuestion normalizeQuestion(String interactionType, String prompt, String choicesText, String resourceText, String answerText) {
        String rawType = toQuestionType(interactionType);
        String rawAnswer = answerText == null ? "" : answerText.trim();
        List<Map<String, String>> choices = "MATCHING".equals(rawType) ? parseMatchingChoices(choicesText) : parseChoices(choicesText);

        if ("THEORY_CARD".equals(rawType)) {
            return parsed("THEORY_CARD", prompt, interactionType, choices, resourceText, Map.of("rawText", rawAnswer, "action", "NEXT"), Map.of("action", "NEXT"));
        }

        if ("OX".equals(rawType)) {
            LinkedHashMap<Integer, String> answers = extractNumberedOxAnswers(rawAnswer);
            if (answers.size() >= 2) {
                List<Map<String, String>> oxChoices = oxCombinationChoices(answers.size());
                String correctId = String.join("", answers.values());
                return parsed("SINGLE_CHOICE", prompt + "\n\uAC01 \uBB38\uC7A5\uC758 O/X \uC870\uD569\uC744 \uACE0\uB974\uC138\uC694.", interactionType, oxChoices, resourceText, answer(rawAnswer, "choiceIds", List.of(correctId)), Map.of());
            }
            String correct = answers.isEmpty() ? firstOx(rawAnswer).orElse("O") : answers.values().iterator().next();
            return parsed("SINGLE_CHOICE", prompt, interactionType, List.of(Map.of("id", "O", "text", "O"), Map.of("id", "X", "text", "X")), resourceText, answer(rawAnswer, "choiceIds", List.of(correct)), Map.of());
        }

        if ("ORDERING".equals(rawType)) {
            List<String> ordered = extractOrderedItemIds(rawAnswer);
            return parsed("ORDERING", prompt, interactionType, choices, resourceText, answer(rawAnswer, "orderedItemIds", ordered), Map.of());
        }

        if ("MATCHING".equals(rawType) || "CLASSIFICATION".equals(rawType)) {
            Optional<MatchingTargetQuestion> targetQuestion = firstSourceTargetAnswer(rawAnswer, choices);
            if (targetQuestion.isPresent()) {
                MatchingTargetQuestion target = targetQuestion.get();
                String revisedPrompt = prompt + "\n" + target.sourceText() + "\n\uC704 \uC0C1\uD669\uC5D0 \uC54C\uB9DE\uC740 \uD56D\uBAA9\uC744 \uACE0\uB974\uC138\uC694.";
                return parsed("SINGLE_CHOICE", revisedPrompt, interactionType, target.choices(), resourceText, answer(rawAnswer, "choiceIds", List.of(target.answerId())), Map.of("focusGroup", target.sourceId()));
            }
            GroupAnswer group = firstGroupAnswer(rawAnswer, choices);
            if (!group.ids().isEmpty()) {
                String type = group.ids().size() > 1 ? "MULTIPLE_CHOICE" : "SINGLE_CHOICE";
                String revisedPrompt = prompt + "\n\uB2E4\uC74C \uC911 '" + group.label() + "'\uC5D0 \uD574\uB2F9\uD558\uB294 \uD56D\uBAA9\uC744 \uBAA8\uB450 \uACE0\uB974\uC138\uC694.";
                return parsed(type, revisedPrompt, interactionType, choices, resourceText, answer(rawAnswer, "choiceIds", group.ids()), Map.of("focusGroup", group.label()));
            }
            return parsed("TEXT_INPUT", prompt + "\n\uC815\uB2F5\uC744 \uD14D\uC2A4\uD2B8\uB85C \uC785\uB825\uD558\uC138\uC694.", interactionType, List.of(), resourceText, Map.of("rawText", rawAnswer), Map.of());
        }

        if ("NUMBER_INPUT".equals(rawType)) {
            List<String> choiceIds = extractChoiceIds(rawAnswer);
            if (!choices.isEmpty() && !choiceIds.isEmpty()) {
                String type = choiceIds.size() > 1 ? "MULTIPLE_CHOICE" : "SINGLE_CHOICE";
                return parsed(type, prompt + "\nChoose the correct option for the selection part.", interactionType, choices, resourceText, answer(rawAnswer, "choiceIds", choiceIds), Map.of());
            }
            Optional<BigDecimal> numberValue = extractPrimaryNumberAnswer(rawAnswer);
            if (numberValue.isPresent() && !looksLikeRangeOrChartAnswer(interactionType, rawAnswer)) {
                String revisedPrompt = firstNumberPrompt(prompt) + "\n\uC22B\uC790\uB9CC \uC785\uB825\uD558\uC138\uC694.";
                Map<String, Object> answer = new LinkedHashMap<>();
                answer.put("rawText", rawAnswer);
                answer.put("numberValue", numberValue.get());
                return parsed("NUMBER_INPUT", revisedPrompt, interactionType, List.of(), resourceText, answer, Map.of());
            }
            return parsed("TEXT_INPUT", prompt + "\nEnter the answer range or value as text.", interactionType, List.of(), resourceText, Map.of("rawText", rawAnswer), Map.of());
        }

        List<String> choiceIds = extractChoiceIds(rawAnswer);
        if (!choices.isEmpty() && !choiceIds.isEmpty()) {
            String type = choiceIds.size() > 1 ? "MULTIPLE_CHOICE" : "SINGLE_CHOICE";
            return parsed(type, prompt, interactionType, choices, resourceText, answer(rawAnswer, "choiceIds", choiceIds), Map.of());
        }
        if (!choices.isEmpty()) {
            return parsed("SINGLE_CHOICE", prompt, interactionType, choices, resourceText, Map.of("rawText", rawAnswer), Map.of());
        }
        return parsed("TEXT_INPUT", prompt, interactionType, List.of(), resourceText, Map.of("rawText", rawAnswer), Map.of());
    }

    private ParsedQuestion parsed(String type, String prompt, String interactionType, List<Map<String, String>> choices,
                                  String resourceText, Map<String, Object> answer, Map<String, Object> extraPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("interactionType", interactionType == null ? "" : interactionType);
        payload.putAll(extraPayload);
        if (!choices.isEmpty()) payload.put("choices", choices);
        if (resourceText != null && !resourceText.isBlank()) payload.put("resource", Map.of("type", "TEXT", "text", resourceText));
        if ("THEORY_CARD".equals(type)) payload.put("action", "NEXT");
        return new ParsedQuestion(type, prompt, writeJson(payload), writeJson(answer));
    }

    private Map<String, Object> answer(String rawText, String key, Object value) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("rawText", rawText == null ? "" : rawText);
        if (value instanceof List<?> list && !list.isEmpty()) {
            answer.put(key, value);
        } else if (!(value instanceof List<?>)) {
            answer.put(key, value);
        }
        return answer;
    }

    private List<Map<String, String>> parseChoices(String choicesText) {
        if (choicesText == null || choicesText.isBlank()) return List.of();
        List<Map<String, String>> choices = new ArrayList<>();
        String normalized = choicesText.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.contains("\n") ? normalized.split("\n") : normalized.split("\\s{2,}");
        int fallbackIndex = 0;
        for (String line : lines) {
            String value = line.trim();
            if (value.isBlank()) continue;
            value = value.replaceFirst("^(카드 목록|카드|조각|설명 카드|설명|개념|지수|단계|용어|상품|상황|헤드라인 카드|키워드|의미|금융기관|감독기관|특징)\\s*:\\s*", "").trim();
            value = value.replaceFirst("^②\\s*", "").trim();
            value = value.replaceFirst("^[·•\\-]\\s*", "").trim();
            if (value.isBlank() || value.endsWith(":")) continue;

            List<Map<String, String>> bracketChoices = extractBracketChoices(value);
            if (!bracketChoices.isEmpty()) {
                choices.addAll(bracketChoices);
                for (Map<String, String> choice : bracketChoices) {
                    fallbackIndex = Math.max(fallbackIndex, choice.get("id").charAt(0) - 'A' + 1);
                }
                continue;
            }

            Matcher inlineChoices = Pattern.compile("([A-Z])\\s*[.)]\\s*(.+?)(?=\\s+[A-Z]\\s*[.)]|$)").matcher(value);
            boolean foundInlineChoice = false;
            while (inlineChoices.find()) {
                choices.add(Map.of("id", inlineChoices.group(1), "text", inlineChoices.group(2).trim()));
                fallbackIndex = Math.max(fallbackIndex + 1, inlineChoices.group(1).charAt(0) - 'A' + 1);
                foundInlineChoice = true;
            }
            if (foundInlineChoice) continue;

            String id = String.valueOf((char) ('A' + fallbackIndex));
            choices.add(Map.of("id", id, "text", value));
            fallbackIndex++;
        }
        return dedupeChoices(choices);
    }

    private List<Map<String, String>> parseMatchingChoices(String choicesText) {
        if (choicesText == null || choicesText.isBlank()) return List.of();
        String normalized = choicesText.replace("\r\n", "\n").replace("\r", "\n");
        int descriptionIndex = normalized.indexOf("설명:");
        String section = descriptionIndex >= 0 ? normalized.substring(descriptionIndex + "설명:".length()) : normalized;

        List<Map<String, String>> choices = new ArrayList<>(extractBracketChoices(section));
        if (!choices.isEmpty()) return dedupeChoices(choices);
        return parseChoices(choicesText);
    }

    private List<Map<String, String>> extractBracketChoices(String value) {
        List<Map<String, String>> choices = new ArrayList<>();
        Matcher matcher = BRACKET_CHOICE_SEGMENT.matcher(value);
        while (matcher.find()) {
            String text = matcher.group(2)
                    .replace("\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!text.isBlank()) {
                choices.add(Map.of("id", matcher.group(1), "text", text));
            }
        }
        return choices;
    }

    private List<Map<String, String>> dedupeChoices(List<Map<String, String>> choices) {
        List<Map<String, String>> deduped = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Map<String, String> choice : choices) {
            String id = choice.get("id");
            String text = choice.get("text");
            if (id == null || id.isBlank() || text == null || text.isBlank() || seen.contains(id)) continue;
            seen.add(id);
            deduped.add(choice);
        }
        return deduped;
    }

    private List<String> extractChoiceIds(String rawText) {
        if (rawText == null || rawText.isBlank()) return List.of();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher matcher = CHOICE_ID.matcher(rawText);
        while (matcher.find()) ids.add(matcher.group(1));
        if (ids.isEmpty()) {
            Matcher plain = PLAIN_CHOICE_ID.matcher(rawText);
            while (plain.find()) ids.add(plain.group(1));
        }
        return new ArrayList<>(ids);
    }

    private List<String> extractOrderedItemIds(String rawText) {
        if (rawText == null || rawText.isBlank()) return List.of();
        Matcher matcher = PLAIN_CHOICE_ID.matcher(rawText);
        List<String> ids = new ArrayList<>();
        while (matcher.find()) ids.add(matcher.group(1));
        return ids;
    }

    private LinkedHashMap<Integer, String> extractNumberedOxAnswers(String rawText) {
        LinkedHashMap<Integer, String> answers = new LinkedHashMap<>();
        if (rawText == null) return answers;
        Matcher matcher = Pattern.compile("([①②③④⑤])\\s*([OX])").matcher(rawText.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            answers.put(circledNumber(matcher.group(1)), matcher.group(2));
        }
        return answers;
    }

    private int circledNumber(String value) {
        return switch (value) {
            case "①" -> 1;
            case "②" -> 2;
            case "③" -> 3;
            case "④" -> 4;
            case "⑤" -> 5;
            default -> 0;
        };
    }

    private Optional<String> firstOx(String rawText) {
        if (rawText == null) return Optional.empty();
        Matcher matcher = Pattern.compile("(?<![A-Z])([OX])(?![A-Z])").matcher(rawText.toUpperCase(Locale.ROOT));
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private List<Map<String, String>> oxCombinationChoices(int count) {
        List<Map<String, String>> choices = new ArrayList<>();
        int size = (int) Math.pow(2, count);
        for (int mask = 0; mask < size; mask++) {
            StringBuilder id = new StringBuilder();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < count; i++) {
                String value = ((mask >> (count - i - 1)) & 1) == 0 ? "O" : "X";
                id.append(value);
                if (i > 0) text.append(" / ");
                text.append(i + 1).append(". ").append(value);
            }
            choices.add(Map.of("id", id.toString(), "text", text.toString()));
        }
        return choices;
    }

    private GroupAnswer firstGroupAnswer(String rawText, List<Map<String, String>> choices) {
        if (rawText == null || rawText.isBlank()) return new GroupAnswer("정답", List.of());
        String[] lines = rawText.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String label = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                List<String> ids = idsFromAnswerFragment(value, choices);
                if (!ids.isEmpty()) return new GroupAnswer(label, ids);
            }
        }
        Matcher pair = Pattern.compile("([^/\\-]+)-\\s*([A-Z])").matcher(rawText);
        if (pair.find()) return new GroupAnswer(pair.group(1).trim(), List.of(pair.group(2)));
        return new GroupAnswer("정답", extractChoiceIds(rawText));
    }

    private Optional<MatchingTargetQuestion> firstSourceTargetAnswer(String rawText, List<Map<String, String>> sourceChoices) {
        if (rawText == null || rawText.isBlank() || sourceChoices.isEmpty()) return Optional.empty();

        LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
        for (String fragment : rawText.replace("\r\n", "\n").replace("\r", "\n").split("\\s+/\\s+|\\n")) {
            Matcher matcher = Pattern.compile("^\\s*([A-Z])\\s*[-\u2013]\\s*(.+?)\\s*$").matcher(fragment.trim());
            if (!matcher.matches()) continue;
            String target = matcher.group(2).trim();
            if (target.matches("[A-Z]")) continue;
            mappings.put(matcher.group(1), target);
        }
        if (mappings.isEmpty()) return Optional.empty();

        Map.Entry<String, String> first = mappings.entrySet().iterator().next();
        Optional<String> sourceText = sourceChoices.stream()
                .filter(choice -> first.getKey().equals(choice.get("id")))
                .map(choice -> choice.get("text"))
                .findFirst();
        if (sourceText.isEmpty()) return Optional.empty();

        List<Map<String, String>> targetChoices = new ArrayList<>();
        String answerId = null;
        int index = 0;
        LinkedHashSet<String> seenTargets = new LinkedHashSet<>();
        for (String target : mappings.values()) {
            if (!seenTargets.add(target)) continue;
            String id = String.valueOf((char) ('A' + index));
            targetChoices.add(Map.of("id", id, "text", target));
            if (target.equals(first.getValue())) {
                answerId = id;
            }
            index++;
        }
        if (targetChoices.size() < 2 || answerId == null) return Optional.empty();
        return Optional.of(new MatchingTargetQuestion(first.getKey(), sourceText.get(), targetChoices, answerId));
    }

    private List<String> idsFromAnswerFragment(String value, List<Map<String, String>> choices) {
        List<String> direct = extractChoiceIds(value);
        if (!direct.isEmpty()) return direct;
        List<String> ids = new ArrayList<>();
        List<String> parts = Arrays.stream(value.split("[,·/]|\\s+및\\s+|\\s+와\\s+|\\s+과\\s+"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        for (Map<String, String> choice : choices) {
            String choiceText = normalizeText(choice.get("text"));
            for (String part : parts) {
                String normalizedPart = normalizeText(part);
                if (!normalizedPart.isBlank() && (choiceText.contains(normalizedPart) || normalizedPart.contains(choiceText))) {
                    ids.add(choice.get("id"));
                    break;
                }
            }
        }
        return ids;
    }

    private Optional<BigDecimal> extractPrimaryNumberAnswer(String rawText) {
        if (rawText == null || rawText.isBlank()) return Optional.empty();
        String candidate = rawText;
        int firstMarker = candidate.indexOf('①');
        if (firstMarker >= 0) {
            int secondMarker = candidate.indexOf('②', firstMarker + 1);
            candidate = secondMarker > firstMarker ? candidate.substring(firstMarker + 1, secondMarker) : candidate.substring(firstMarker + 1);
        } else if (candidate.contains(":")) {
            candidate = candidate.substring(candidate.indexOf(':') + 1);
            int newline = candidate.indexOf('\n');
            if (newline > 0) candidate = candidate.substring(0, newline);
        } else if (candidate.contains("=")) {
            candidate = candidate.substring(candidate.lastIndexOf('=') + 1);
        }
        Matcher matcher = NUMBER_PATTERN.matcher(candidate);
        BigDecimal last = null;
        while (matcher.find()) last = new BigDecimal(matcher.group().replace(",", ""));
        return Optional.ofNullable(last);
    }

    private boolean looksLikeRangeOrChartAnswer(String interactionType, String rawAnswer) {
        String text = ((interactionType == null ? "" : interactionType) + " " + (rawAnswer == null ? "" : rawAnswer));
        return text.contains("차트") || text.contains("터치") || text.contains("구간") || text.contains("~") || text.contains("현재");
    }

    private String firstNumberPrompt(String prompt) {
        int marker = prompt.indexOf('②');
        if (marker > 0) return prompt.substring(0, marker).trim();
        return prompt;
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        return value.toUpperCase(Locale.ROOT).replaceAll("[\\s,._:()\\[\\]/%·\\-]", "");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize curriculum JSON", e);
        }
    }

    private String nullableStringCell(Cell cell) {
        String value = stringCell(cell);
        return value.isBlank() ? null : value;
    }

    private String stringCell(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (value == Math.rint(value)) return String.valueOf((long) value);
            return String.valueOf(value);
        }
        if (cell.getCellType() == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
    }

    private double numericCell(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        return Double.parseDouble(stringCell(cell));
    }

    private String toSessionType(String contentType) {
        return switch (contentType) {
            case "\uC774\uB860" -> "THEORY";
            case "\uC6A9\uC5B4" -> "TERM_MATCH";
            case "\uB4DC\uB9B4" -> "DRILL";
            case "\uC5F0\uACB0" -> "CONNECTION";
            case "\uB370\uC774\uD130" -> "DATA";
            default -> "DRILL";
        };
    }

    private String toQuestionType(String interactionType) {
        if (interactionType == null || interactionType.isBlank()) return "SINGLE_CHOICE";
        if (interactionType.contains("\uD0ED")) return "THEORY_CARD";
        if (interactionType.contains("OX")) return "OX";
        if (interactionType.contains("\uAC1D\uAD00\uC2DD")) return "SINGLE_CHOICE";
        if (interactionType.contains("\uC21C\uC11C")) return "ORDERING";
        if (interactionType.contains("\uCE74\uB4DC \uB9E4\uCE6D")) return "MATCHING";
        if (interactionType.contains("\uB4DC\uB798\uADF8")) return "CLASSIFICATION";
        if (interactionType.contains("\uCC28\uD2B8")) return "CHART_POINT";
        if (interactionType.contains("\uC22B\uC790")) return "NUMBER_INPUT";
        if (interactionType.contains("\uBC29\uD5A5")) return "DIRECTION_SELECT";
        if (interactionType.contains("\uBE48\uCE78")) return "TEXT_INPUT";
        return "SINGLE_CHOICE";
    }

    private record ParsedQuestion(String type, String prompt, String payloadJson, String answerJson) {}

    private record GroupAnswer(String label, List<String> ids) {}

    private record MatchingTargetQuestion(String sourceId, String sourceText, List<Map<String, String>> choices, String answerId) {}
}
