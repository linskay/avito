package ru.avito.hackathon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.model.FoundMicrocategory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для {@link CategoryMatcherService}.
 */
@DisplayName("Тесты сервиса сопоставления категорий")
class CategoryMatcherServiceTest {

    private CategoryMatcherService service;
    private List<Microcategory> dictionary;

    @BeforeEach
    void setUp() {
        service = new CategoryMatcherService();
        dictionary = List.of(
                new Microcategory(101, "Сантехника", List.of("разводка труб", "установка сантехники")),
                new Microcategory(102, "Электрика", List.of("замена проводки", "монтаж розеток"))
        );
    }

    @Test
    @DisplayName("Находит два совпадения в разных предложениях")
    void testFindMicrocategoriesInText() {
        String text = "Делаем ремонт. Также можем предложить монтаж розеток. Возможна установка сантехники под ключ!";

        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, dictionary);

        assertEquals(2, result.size());
        assertEquals(102, result.get(0).microcategory().mcId());
        assertEquals("монтаж розеток", result.get(0).matchedPhrase());
        assertEquals(101, result.get(1).microcategory().mcId());
        assertEquals("установка сантехники", result.get(1).matchedPhrase());
    }

    @Test
    @DisplayName("Возвращает пустой список если нет совпадений")
    void testNoMatch() {
        String text = "Просто клеим обои и кладем ламинат.";

        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, List.of(
                new Microcategory(101, "Сантехника", List.of("разводка труб"))
        ));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Поиск нечувствителен к регистру")
    void testCaseInsensitiveSearch() {
        String text = "Выполняем МОНТАЖ РОЗЕТОК по любому адресу.";

        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, dictionary);

        assertEquals(1, result.size());
        assertEquals(102, result.get(0).microcategory().mcId());
    }

    @Test
    @DisplayName("Пустой словарь — всегда пустой результат")
    void testEmptyDictionary() {
        String text = "Установка сантехники и электрики.";

        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Пустой текст — пустой результат")
    void testEmptyText() {
        List<FoundMicrocategory> result = service.findMicrocategoriesInText("", dictionary);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Одна категория не дублируется если встречается в двух предложениях")
    void testNoDuplicateCategoryAcrossSentences() {
        String text = "Разводка труб в ванной. Также разводка труб на кухне.";

        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, dictionary);

        long count = result.stream()
                .filter(r -> r.microcategory().mcId() == 101)
                .count();
        assertEquals(1, count, "Категория должна совпасть только один раз, даже если встречается в нескольких предложениях");
    }

    @Test
    @DisplayName("Контекст предложения (sentenceContext) корректно заполняется")
    void testSentenceContextIsCorrect() {
        String text = "Делаем обои. Монтаж розеток по запросу.";

        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, dictionary);

        assertEquals(1, result.size());
        assertEquals("Монтаж розеток по запросу.", result.get(0).sentenceContext().trim());
    }
}
