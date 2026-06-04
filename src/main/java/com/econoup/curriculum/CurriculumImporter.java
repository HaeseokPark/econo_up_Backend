package com.econoup.curriculum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
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
        long existingSessionCount = sessionRepository.count();
        if (!importOnStartup) {
            log.info("Curriculum import skipped: app.curriculum.import-on-startup=false");
            return;
        }
        if (existingSessionCount > 0) {
            log.info("Curriculum import skipped: {} sessions already exist", existingSessionCount);
            return;
        }

        try (InputStream inputStream = curriculumResource.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            log.info("Curriculum import started: {} sheets found", workbook.getNumberOfSheets());
            importSheet(workbook, 1, new CategoryEntity("ECONOMY", "\uACBD\uC81C \uC0C1\uC2DD", "\uC138\uC0C1 \uB3CC\uC544\uAC00\uB294 \uD750\uB984\uC744 \uC77D\uB294 \uAE30\uBCF8\uAE30", 1));
            importSheet(workbook, 2, new CategoryEntity("SAVING", "\uC800\uCD95", "\uC6D4\uAE09\uC744 \uC9C0\uD0A4\uACE0 \uBD88\uB9AC\uB294 \uCCAB \uB8E8\uD2F4", 2));
            importSheet(workbook, 3, new CategoryEntity("STOCK", "\uC8FC\uC2DD", "\uAE30\uC5C5\uACFC \uC2DC\uC7A5\uC744 \uC77D\uB294 \uD22C\uC790 \uAE30\uCD08", 3));
            log.info("Curriculum import finished: {} sessions and {} questions saved",
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
        int importedSessionCount = 0;

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
                }
                continue;
            }

            if (currentStage != null && !first.isBlank() && !sessionCode.isBlank()) {
                int sequence = (int) numericCell(row.getCell(0));
                if (sessionRepository.findByCode(sessionCode).isPresent()) {
                    continue;
                }
                String contentType = stringCell(row.getCell(2));
                String interactionType = stringCell(row.getCell(3));
                String questionType = toQuestionType(interactionType);
                String choicesText = nullableStringCell(row.getCell(5));
                String resourceText = nullableStringCell(row.getCell(6));
                String answerText = nullableStringCell(row.getCell(7));
                SessionEntity session = sessionRepository.save(new SessionEntity(
                        currentStage,
                        sessionCode,
                        sequence,
                        toSessionType(contentType),
                        interactionType
                ));
                questionRepository.save(new QuestionEntity(
                        session,
                        1,
                        questionType,
                        stringCell(row.getCell(4)),
                        toPayloadJson(questionType, interactionType, choicesText, resourceText),
                        toAnswerJson(questionType, answerText),
                        nullableStringCell(row.getCell(8))
                ));
                importedSessionCount++;
            }
        }

        log.info("Curriculum sheet imported: sheet='{}', category={}, sessions={}",
                sheet.getSheetName(), category.code, importedSessionCount);
    }

    private String toPayloadJson(String questionType, String interactionType, String choicesText, String resourceText) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("interactionType", interactionType == null ? "" : interactionType);
        List<Map<String, String>> choices = parseChoices(choicesText);
        if (!choices.isEmpty()) {
            payload.put("choices", choices);
        }
        if (resourceText != null && !resourceText.isBlank()) {
            payload.put("resource", Map.of("type", "TEXT", "text", resourceText));
        }
        if ("THEORY_CARD".equals(questionType)) {
            payload.put("action", "NEXT");
        }
        return writeJson(payload);
    }

    private String toAnswerJson(String questionType, String answerText) {
        Map<String, Object> answer = new LinkedHashMap<>();
        String rawText = answerText == null ? "" : answerText.trim();
        answer.put("rawText", rawText);
        if ("THEORY_CARD".equals(questionType)) {
            answer.put("action", "NEXT");
            return writeJson(answer);
        }

        List<String> choiceIds = extractChoiceIds(rawText);
        if (!choiceIds.isEmpty()) {
            answer.put("choiceIds", choiceIds);
        }

        if ("ORDERING".equals(questionType)) {
            List<String> orderedItemIds = extractOrderedItemIds(rawText);
            if (!orderedItemIds.isEmpty()) {
                answer.put("orderedItemIds", orderedItemIds);
            }
        }

        if ("NUMBER_INPUT".equals(questionType)) {
            Optional<BigDecimal> numberValue = extractNumber(rawText);
            numberValue.ifPresent(value -> answer.put("numberValue", value));
        }
        return writeJson(answer);
    }

    private List<Map<String, String>> parseChoices(String choicesText) {
        if (choicesText == null || choicesText.isBlank()) {
            return List.of();
        }

        if (choicesText.contains("O") && choicesText.contains("X") && choicesText.contains("/")) {
            return List.of(
                    Map.of("id", "O", "text", "O"),
                    Map.of("id", "X", "text", "X")
            );
        }

        List<Map<String, String>> choices = new ArrayList<>();
        String normalized = choicesText.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.contains("\n") ? normalized.split("\n") : normalized.split("/");
        int fallbackIndex = 0;
        for (String line : lines) {
            String value = line.trim();
            if (value.isBlank()) continue;
            if (value.startsWith("카드 목록:") || value.startsWith("카드:")) {
                value = value.replaceFirst("^카드 목록:\\s*", "").replaceFirst("^카드:\\s*", "");
            }
            value = value.replaceFirst("^[①②③④⑤⑥⑦⑧⑨]\\s*", "");

            for (String part : value.split("\\s*·\\s*")) {
                String item = part.trim();
                if (item.isBlank() || "카드 목록:".equals(item)) continue;
                Matcher inlineChoices = Pattern.compile("([A-Z])\\s*[.)]\\s*([^A-Z]+?)(?=\\s+[A-Z]\\s*[.)]|$)").matcher(item);
                boolean foundInlineChoice = false;
                while (inlineChoices.find()) {
                    choices.add(Map.of("id", inlineChoices.group(1), "text", inlineChoices.group(2).trim()));
                    fallbackIndex++;
                    foundInlineChoice = true;
                }
                if (foundInlineChoice) {
                    continue;
                }

                Matcher matcher = Pattern.compile("^([A-Z]|[①②③④⑤⑥⑦⑧⑨])\\s*[.)]?\\s*(.+)$").matcher(item);
                String id = String.valueOf((char) ('A' + fallbackIndex));
                String text = item;
                if (matcher.matches()) {
                    id = normalizeChoiceId(matcher.group(1), fallbackIndex);
                    text = matcher.group(2).trim();
                }
                choices.add(Map.of("id", id, "text", text));
                fallbackIndex++;
            }
        }
        return choices;
    }

    private List<String> extractChoiceIds(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("(?<![A-Za-z])([A-Z])\\s*[.)]").matcher(rawText);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        if (ids.isEmpty()) {
            Matcher plain = Pattern.compile("(?<![A-Za-z])([A-Z])(?![A-Za-z])").matcher(rawText);
            while (plain.find()) {
                ids.add(plain.group(1));
            }
        }
        return new ArrayList<>(ids);
    }

    private List<String> extractOrderedItemIds(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        Matcher matcher = Pattern.compile("(?<![A-Za-z])([A-Z])(?![A-Za-z])").matcher(rawText);
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    private Optional<BigDecimal> extractNumber(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("-?\\d[\\d,]*(?:\\.\\d+)?").matcher(rawText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(matcher.group().replace(",", "")));
    }

    private String normalizeChoiceId(String value, int fallbackIndex) {
        return switch (value) {
            case "①" -> "A";
            case "②" -> "B";
            case "③" -> "C";
            case "④" -> "D";
            case "⑤" -> "E";
            case "⑥" -> "F";
            case "⑦" -> "G";
            case "⑧" -> "H";
            case "⑨" -> "I";
            default -> value == null || value.isBlank() ? String.valueOf((char) ('A' + fallbackIndex)) : value;
        };
    }

    private String writeJson(Map<String, Object> value) {
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
            if (value == Math.rint(value)) {
                return String.valueOf((long) value);
            }
            return String.valueOf(value);
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
    }

    private double numericCell(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
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
        if (interactionType.contains("\uC22B\uC790")) return "NUMBER_INPUT";
        if (interactionType.contains("\uCE74\uB4DC \uB9E4\uCE6D")) return "MATCHING";
        if (interactionType.contains("\uB4DC\uB798\uADF8")) return "CLASSIFICATION";
        if (interactionType.contains("\uC21C\uC11C")) return "ORDERING";
        if (interactionType.contains("\uBC29\uD5A5")) return "DIRECTION_SELECT";
        if (interactionType.contains("\uCC28\uD2B8")) return "CHART_POINT";
        if (interactionType.contains("\uBE48\uCE78")) return "TEXT_INPUT";
        return "SINGLE_CHOICE";
    }
}
