package ru.avito.hackathon.service;

import org.junit.jupiter.api.Test;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.model.FoundMicrocategory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMatcherServiceTest {

    private final CategoryMatcherService service = new CategoryMatcherService();

    @Test
    void testFindMicrocategoriesInText() {
        // Подготовка данных
        Microcategory plumbing = new Microcategory(101, "Сантехника", List.of("разводка труб", "установка сантехники"));
        Microcategory electrics = new Microcategory(102, "Электрика", List.of("замена проводки", "монтаж розеток"));
        List<Microcategory> dictionary = List.of(plumbing, electrics);

        String text = "Делаем ремонт. Также можем предложить монтаж розеток. Возможна установка сантехники под ключ!";

        // Исполнение
        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, dictionary);

        // Проверки
        assertEquals(2, result.size());

        FoundMicrocategory first = result.get(0);
        assertEquals(102, first.microcategory().mcId());
        assertEquals("монтаж розеток", first.matchedPhrase());
        assertEquals("Также можем предложить монтаж розеток.", first.sentenceContext());

        FoundMicrocategory second = result.get(1);
        assertEquals(101, second.microcategory().mcId());
        assertEquals("установка сантехники", second.matchedPhrase());
        assertEquals("Возможна установка сантехники под ключ!", second.sentenceContext());
    }

    @Test
    void testNoMatch() {
        Microcategory plumbing = new Microcategory(101, "Сантехника", List.of("разводка труб", "установка сантехники"));
        
        String text = "Просто клеим обои и кладем ламинат.";

        List<FoundMicrocategory> result = service.findMicrocategoriesInText(text, List.of(plumbing));

        assertTrue(result.isEmpty());
    }
}
