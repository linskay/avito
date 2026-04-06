package ru.avito.hackathon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.dto.Draft;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.dto.SplitResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для {@link DependencyTraversalService}.
 * <p>
 * Охватывают ключевые сценарии работы эвристического алгоритма:
 * наличие и отсутствие маркеров обособленности, чувствительность к регистру,
 * поведение при пустом словаре, обработку bullet-list формата,
 * дедупликацию черновиков по mcId, а также разделители «/» и «+».
 */
@DisplayName("Тестирование эвристического алгоритма выделения услуг")
class DependencyTraversalServiceTest {

    private DependencyTraversalService traversalService;

    /** Тестовый словарь микрокатегорий, используемый в большинстве тестов */
    private List<Microcategory> testDictionary;

    @BeforeEach
    void setUp() {
        traversalService = new DependencyTraversalService();
        testDictionary = List.of(
                new Microcategory(101, "Сантехника", List.of("сантехнику", "установка сантехники", "разводка труб")),
                new Microcategory(102, "Электрика", List.of("электрику", "монтаж электрики", "установка розеток")),
                new Microcategory(103, "Укладка плитки", List.of("укладка плитки", "плиточные работы")),
                new Microcategory(104, "Гипсокартон", List.of("гипсокартон", "перегородки из гкл"))
        );
    }

    // ===========================
    // Позитивные сценарии (split)
    // ===========================

    @Test
    @DisplayName("Сценарий turnkey_split: маркер 'отдельно' — создаётся черновик")
    void testDetermineSplits_withIndependentService_shouldSplit() {
        Ad ad = new Ad(1, 999, "Ремонт под ключ", "Делаем ремонт. Также отдельно выполняем сантехнику.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertTrue(result.shouldSplit(), "Должно быть предложено разделение");
        assertEquals(1, result.drafts().size(), "Должен быть создан 1 черновик");

        Draft draft = result.drafts().get(0);
        assertEquals(101, draft.mcId(), "ID категории Сантехника");
        assertEquals("Сантехника", draft.mcTitle());

        assertTrue(result.detectedMcIds().contains(101), "Сантехника должна быть найдена в тексте");
    }

    @Test
    @DisplayName("Сценарий multi_service_split: несколько маркеров — несколько черновиков")
    void testDetermineSplits_multipleServices_multiplesDrafts() {
        Ad ad = new Ad(2, 999, "Ремонт", "Делаем ремонт. Также отдельно выполняем сантехнику. Можем отдельно монтаж электрики.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertTrue(result.shouldSplit());
        assertEquals(2, result.drafts().size(), "Два разных черновика: сантехника и электрика");

        List<Integer> foundIds = result.drafts().stream().map(Draft::mcId).toList();
        assertTrue(foundIds.contains(101), "Должна быть найдена сантехника");
        assertTrue(foundIds.contains(102), "Должна быть найдена электрика");
    }

    @Test
    @DisplayName("Сценарий bullets_mixed: маркер 'как отдельную услугу' в списке")
    void testDetermineSplits_bulletListFormat_shouldSplit() {
        Ad ad = new Ad(3, 999, "Плитка", """
                Укладка плитки в ванной.
                - установка розеток как отдельную услугу
                """);

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertTrue(result.shouldSplit(), "Маркер 'как отдельную услугу' должен обнаруживать черновик");
        assertEquals(102, result.drafts().get(0).mcId(), "Должна быть найдена электрика");
    }

    @Test
    @DisplayName("Сценарий noisy_short: разделители «/» и «+»")
    void testDetermineSplits_noisyShortFormat_shouldSplit() {
        Ad ad = new Ad(4, 999, "Полы", "укладка плитки /установка сантехники также отдельно выезд лс");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertTrue(result.shouldSplit());
        // Должна быть найдена сантехника (маркер «также отдельно» там присутствует)
        List<Integer> foundIds = result.drafts().stream().map(Draft::mcId).toList();
        assertTrue(foundIds.contains(101), "Сантехника должна быть в черновиках");
    }

    @Test
    @DisplayName("Поиск нечувствителен к регистру (ДОПОЛНИТЕЛЬНО, Отдельно)")
    void testDetermineSplits_caseInsensitiveMarker() {
        Ad ad = new Ad(5, 999, "Уборка", "Основная услуга. ДОПОЛНИТЕЛЬНО установка розеток.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertTrue(result.shouldSplit(), "Маркер в верхнем регистре должен обнаруживаться");
        assertEquals(102, result.drafts().get(0).mcId());
    }

    @Test
    @DisplayName("Ключевая фраза нечувствительна к регистру (УСТАНОВКА РОЗЕТОК)")
    void testDetermineSplits_caseInsensitiveKeyPhrase() {
        Ad ad = new Ad(6, 999, "Услуги", "Ремонт. Также отдельно УСТАНОВКА РОЗЕТОК.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertTrue(result.shouldSplit());
        assertEquals(102, result.drafts().get(0).mcId(), "Ключевая фраза в верхнем регистре должна найти электрику");
    }

    // ===========================
    // Негативные сценарии (no split)
    // ===========================

    @Test
    @DisplayName("Нет маркера обособленности — черновик не создаётся")
    void testDetermineSplits_noMarker_shouldNotSplit() {
        Ad ad = new Ad(7, 999, "Ремонт", "Делаем ремонт квартир и сантехнику заодно.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertFalse(result.shouldSplit(), "Без маркера 'отдельно' разделения нет");
        assertTrue(result.drafts().isEmpty());

        assertTrue(result.detectedMcIds().contains(101), "Сантехника должна быть найдена в тексте, несмотря на отсутствие маркера");
    }

    @Test
    @DisplayName("Маркер есть, но словарь не содержит совпадений — черновик не создаётся")
    void testDetermineSplits_markerPresentButNoDictionaryMatch() {
        Ad ad = new Ad(8, 999, "Сварщик", "Варю трубы. Также отдельно копаю ямы для труб.");
        // В словаре нет «копаю ямы»

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertFalse(result.shouldSplit(), "Без совпадения в словаре черновик не создаётся");
        assertTrue(result.drafts().isEmpty());
    }

    @Test
    @DisplayName("Пустой словарь — всегда shouldSplit=false")
    void testDetermineSplits_emptyDictionary() {
        Ad ad = new Ad(9, 999, "Ремонт", "Ремонт. Также отдельно выполняем сантехнику.");

        SplitResult result = traversalService.determineSplits(ad, List.of());

        assertFalse(result.shouldSplit(), "При пустом словаре черновик не создёт");
        assertTrue(result.drafts().isEmpty());
    }

    @Test
    @DisplayName("Turnkey_no_split: услуга в контексте комплексного ремонта — не разделяем")
    void testDetermineSplits_turnkeyNoSplit() {
        Ad ad = new Ad(10, 999, "Ремонт под ключ",
                "Ремонт квартир под ключ. Выполняем установку сантехники и электрику в составе ремонта. " +
                "Ищу заказы именно на комплекс, по отдельным видам работ не выезжаю.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        // «по отдельным видам работ не выезжаю» содержит «отдельно», но с отрицанием.
        // Это сложный кейс — тест фиксирует текущее поведение алгоритма (false negative).
        assertFalse(result.shouldSplit(), "Ключевая фраза 'по отдельным видам работ не выезжаю' не должна создавать черновик");
    }

    // ===========================
    // Сценарии дедупликации
    // ===========================

    @Test
    @DisplayName("Дедупликация: одна категория не дублируется, даже если маркеров несколько")
    void testDetermineSplits_deduplication_noDuplicateDrafts() {
        Ad ad = new Ad(11, 999, "Услуги", "Ремонт. Также отдельно разводка труб. Можем отдельно сантехнику.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        // Обе фразы «разводка труб» и «сантехнику» относятся к mcId=101, дубликата быть не должно
        long count101 = result.drafts().stream().filter(d -> d.mcId() == 101).count();
        assertEquals(1, count101, "Категория 101 (Сантехника) должна встречаться в черновиках только 1 раз");
    }

    @Test
    @DisplayName("Дедупликация: исходная категория объявления не попадает в черновики")
    void testDetermineSplits_sourceCategoryExcluded() {
        // Объявление уже принадлежит категории 101 (Сантехника)
        Ad ad = new Ad(12, 101, "Сантехника", "Сантехника. Также отдельно разводка труб. Можем отдельно установка розеток.");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        // Несмотря на маркер, категория 101 не должна попасть в черновики
        boolean has101 = result.drafts().stream().anyMatch(d -> d.mcId() == 101);
        assertFalse(has101, "Исходная категория объявления не должна быть в черновиках");
        // Но электрика (102) должна быть
        boolean has102 = result.drafts().stream().anyMatch(d -> d.mcId() == 102);
        assertTrue(has102, "Электрика должна быть в черновиках");
    }

    // ===========================
    // Форматирование текста
    // ===========================

    @Test
    @DisplayName("Ведущие символы «-» и «•» удаляются из начала текста черновика")
    void testDetermineSplits_leadingBulletSymbolsRemoved() {
        Ad ad = new Ad(13, 999, "Строитель", "Услуги. \n- установка розеток как отдельную услугу");

        SplitResult result = traversalService.determineSplits(ad, testDictionary);

        assertTrue(result.shouldSplit());
        String text = result.drafts().get(0).text();
        assertFalse(text.startsWith("-"), "Ведущий символ '-' должен быть удалён из текста черновика");
    }
}
