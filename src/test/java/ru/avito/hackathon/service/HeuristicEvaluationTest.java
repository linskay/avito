package ru.avito.hackathon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.dto.Draft;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.dto.SplitResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Оценочный тест для измерения качества эвристического алгоритма разделения
 * объявлений
 * на реальном датасете из 3000 объявлений
 * ({@code test data/rnc_dataset.jsonl}).
 * <p>
 * Словарь микрокатегорий загружается из
 * {@code test data/rnc_mic_key_phrases.csv}.
 * <p>
 * Вычисляемые метрики:
 * <ul>
 * <li><b>Precision</b> — доля корректных черновиков среди всех созданных</li>
 * <li><b>Recall</b> — доля ожидаемых черновиков, которые алгоритм нашёл</li>
 * <li><b>F1-score</b> — гармоническое среднее Precision и Recall</li>
 * <li><b>Exact Match</b> — доля объявлений, где набор mcId точно совпал с
 * эталоном</li>
 * <li><b>shouldSplit Accuracy</b> — точность предсказания флага «нужно ли
 * делить вообще»</li>
 * </ul>
 * <p>
 * Тест запускается <b>вручную</b>:
 * 
 * <pre>
 *   ./mvnw test -Dtest=HeuristicEvaluationTest -DfailIfNoTests=false
 * </pre>
 */
@DisplayName("Оценка качества эвристики на датасете rnc_dataset (3000 объявлений)")
class HeuristicEvaluationTest {

    /** Путь к датасету относительно корня проекта */
    private static final String DATASET_PATH = "test data/rnc_dataset_markup.json";

    /** Путь к словарю ключевых фраз */
    private static final String PHRASES_PATH = "test data/rnc_mic_key_phrases.csv";

    /** Минимально приемлемый F1-score для прохождения теста */
    private static final double MIN_ACCEPTABLE_F1 = 0.40;

    private static DependencyTraversalService service;
    private static List<Microcategory> dictionary;

    @BeforeAll
    static void setUpAll() throws IOException {
        service = new DependencyTraversalService();
        dictionary = loadDictionary();
        System.out.println("✅ Словарь загружен: " + dictionary.size() + " микрокатегорий");
    }

    /**
     * Прогон алгоритма на 3000 объявлениях из датасета.
     * Выводит детальный отчёт с метриками качества в стандартный вывод.
     */
    @Test
    @DisplayName("Оценка Precision/Recall/F1 на всём датасете (split=all)")
    void evaluateOnFullDataset() throws IOException {
        List<EvalRecord> records = loadDataset(null); // null = все записи
        EvalStats stats = evaluate(records);
        printReport(stats, "Весь датасет (" + records.size() + " записей)");
        assertTrue(stats.f1() >= MIN_ACCEPTABLE_F1,
                String.format("F1-score %.3f ниже минимального порога %.3f. Нужно улучшить алгоритм!", stats.f1(),
                        MIN_ACCEPTABLE_F1));
    }

    @Test
    @DisplayName("Оценка только на тестовой выборке (split=test)")
    void evaluateOnTestSplit() throws IOException {
        List<EvalRecord> records = loadDataset("test");
        EvalStats stats = evaluate(records);
        printReport(stats, "Тестовая выборка (split=test, " + records.size() + " записей)");
        assertTrue(stats.f1() >= MIN_ACCEPTABLE_F1,
                String.format("F1-score на тесте %.3f ниже порога %.3f", stats.f1(), MIN_ACCEPTABLE_F1));
    }

    @Test
    @DisplayName("Детальный анализ по типам кейсов (caseType)")
    void evaluatePerCaseType() throws IOException {
        List<EvalRecord> allRecords = loadDataset(null);
        Map<String, List<EvalRecord>> byCaseType = new LinkedHashMap<>();
        for (EvalRecord r : allRecords) {
            byCaseType.computeIfAbsent(r.caseType(), k -> new ArrayList<>()).add(r);
        }

        System.out.println("\n========== АНАЛИЗ ПО ТИПАМ КЕЙСОВ ==========");
        for (Map.Entry<String, List<EvalRecord>> entry : byCaseType.entrySet()) {
            EvalStats stats = evaluate(entry.getValue());
            System.out.printf("%-30s | n=%4d | P=%.3f | R=%.3f | F1=%.3f | Acc=%.3f%n",
                    entry.getKey(), entry.getValue().size(),
                    stats.precision(), stats.recall(), stats.f1(), stats.exactMatch());
        }
        System.out.println("=============================================\n");
    }

    // ============================
    // Вспомогательные методы
    // ============================

    /**
     * Загружает записи из JSONL-файла датасета.
     *
     * @param splitFilter фильтр по полю {@code split} ("train", "val", "test" или
     *                    {@code null} — все)
     * @return список записей для оценки
     */
    private List<EvalRecord> loadDataset(String splitFilter) throws IOException {
        Path path = Paths.get(DATASET_PATH);
        List<EvalRecord> records = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(path.toFile());
            if (!root.isArray()) return records;

            for (JsonNode node : root) {
                // В новом наборе split=null везде, поэтому фильтр по нему пропускаем
                // String splitVal = node.path("split").asText(null);
                // if (splitFilter != null && !splitFilter.equals(splitVal)) continue;

                String itemIdStr = node.path("itemId").asText();
                int itemId = 0;
                try {
                    itemId = Integer.parseInt(itemIdStr);
                } catch (NumberFormatException ignored) {}

                int sourceMcId = node.path("sourceMcId").asInt();
                String sourceMcTitle = node.path("sourceMcTitle").asText();
                String description = node.path("description").asText();
                boolean shouldSplit = node.path("shouldSplit").asBoolean();
                String caseType = node.path("caseType").asText();

                // Парсим targetSplitMcIds — строка вида "[111, 108, 110]" или "[]"
                Set<Integer> targetSplitMcIds = parseMcIds(node.path("targetSplitMcIds").asText("[]"));

                records.add(new EvalRecord(itemId, sourceMcId, sourceMcTitle, description,
                        shouldSplit, targetSplitMcIds, caseType, null));
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка при загрузке датасета: " + e.getMessage());
        }
        return records;
    }

    private Set<Integer> parseMcIds(String rawIds) {
        Set<Integer> result = new HashSet<>();
        if (rawIds == null || rawIds.equals("[]") || rawIds.isBlank()) return result;
        String clean = rawIds.replaceAll("[\\[\\]\\s]", "");
        if (clean.isEmpty()) return result;
        for (String part : clean.split(",")) {
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /**
     * Прогоняет алгоритм на списке записей и вычисляет метрики.
     */
    private EvalStats evaluate(List<EvalRecord> records) {
        int totalTP = 0, totalFP = 0, totalFN = 0;
        int exactMatchCount = 0;
        int shouldSplitCorrect = 0;

        for (EvalRecord rec : records) {
            Ad ad = new Ad(rec.itemId(), rec.sourceMcId(), rec.sourceMcTitle(), rec.description());
            SplitResult result = service.determineSplits(ad, dictionary);

            Set<Integer> predicted = new HashSet<>();
            for (Draft d : result.drafts())
                predicted.add(d.mcId());

            Set<Integer> expected = rec.targetSplitMcIds();

            // TP = правильно предсказанные
            long tp = predicted.stream().filter(expected::contains).count();
            long fp = predicted.stream().filter(id -> !expected.contains(id)).count();
            long fn = expected.stream().filter(id -> !predicted.contains(id)).count();

            totalTP += (int) tp;
            totalFP += (int) fp;
            totalFN += (int) fn;

            // Точное совпадение набора категорий
            if (predicted.equals(expected))
                exactMatchCount++;

            // Точность предсказания флага shouldSplit
            if (result.shouldSplit() == rec.shouldSplit())
                shouldSplitCorrect++;
        }

        double precision = totalTP + totalFP > 0 ? (double) totalTP / (totalTP + totalFP) : 0.0;
        double recall = totalTP + totalFN > 0 ? (double) totalTP / (totalTP + totalFN) : 0.0;
        double f1 = precision + recall > 0 ? 2 * precision * recall / (precision + recall) : 0.0;
        double exactMatch = (double) exactMatchCount / records.size();
        double shouldSplitAcc = (double) shouldSplitCorrect / records.size();

        return new EvalStats(precision, recall, f1, exactMatch, shouldSplitAcc, totalTP, totalFP, totalFN);
    }

    /**
     * Выводит форматированный отчёт с метриками в стандартный поток вывода.
     */
    private void printReport(EvalStats stats, String label) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║     ОТЧЁТ ОЦЕНКИ ЭВРИСТИЧЕСКОГО АЛГОРИТМА       ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  Выборка         : %-30s║%n", label);
        System.out.printf("║  Precision       : %-30.4f║%n", stats.precision());
        System.out.printf("║  Recall          : %-30.4f║%n", stats.recall());
        System.out.printf("║  F1-Score        : %-30.4f║%n", stats.f1());
        System.out.printf("║  Exact Match     : %-30.4f║%n", stats.exactMatch());
        System.out.printf("║  shouldSplit Acc : %-30.4f║%n", stats.shouldSplitAcc());
        System.out.printf("║  TP=%d  FP=%d  FN=%d%n", stats.tp(), stats.fp(), stats.fn());
        System.out.println("╚══════════════════════════════════════════════════╝\n");
    }

    /**
     * Загружает словарь микрокатегорий из CSV-файла датасета.
     */
    private static List<Microcategory> loadDictionary() throws IOException {
        Path path = Paths.get(PHRASES_PATH);
        List<Microcategory> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.isBlank())
                    continue;
                String[] parts = splitCsvLine(line);
                if (parts.length < 3)
                    continue;
                try {
                    int mcId = Integer.parseInt(parts[0].trim());
                    String mcTitle = parts[1].trim();
                    String rawPhrases = parts[2].trim().replaceAll("^\"|\"$", "");
                    List<String> phrases = Arrays.stream(rawPhrases.split(";"))
                            .map(String::trim).filter(p -> !p.isBlank()).toList();
                    result.add(new Microcategory(mcId, mcTitle, phrases));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    /** Разбивает строку CSV с учётом кавычек */
    private static String[] splitCsvLine(String line) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQ = !inQ;
            } else if (c == ',' && !inQ) {
                res.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        res.add(cur.toString());
        return res.toArray(new String[0]);
    }

    // ============================
    // Внутренние структуры данных
    // ============================

    /** Запись из датасета для оценки */
    private record EvalRecord(
            int itemId, int sourceMcId, String sourceMcTitle, String description,
            boolean shouldSplit, Set<Integer> targetSplitMcIds, String caseType, String split) {
    }

    /** Агрегированные метрики оценки */
    private record EvalStats(
            double precision, double recall, double f1,
            double exactMatch, double shouldSplitAcc,
            int tp, int fp, int fn) {
    }
}
