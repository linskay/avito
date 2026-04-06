package ru.avito.hackathon.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.avito.hackathon.dto.Microcategory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для {@link MicrocategoryLoaderService}.
 * Проверяет загрузку CSV-словаря из classpath-ресурса.
 */
@DisplayName("Тесты загрузки словаря микрокатегорий")
class MicrocategoryLoaderServiceTest {

    @Test
    @DisplayName("Словарь успешно загружается из microcategories.csv")
    void whenCsvExists_thenDictionaryIsNotEmpty() {
        MicrocategoryLoaderService service = new MicrocategoryLoaderService();
        service.loadDictionary();

        List<Microcategory> dictionary = service.getDictionary();

        assertNotNull(dictionary, "Словарь не должен быть null");
        assertFalse(dictionary.isEmpty(), "Словарь не должен быть пустым — CSV файл должен содержать категории");
    }

    @Test
    @DisplayName("Каждая загруженная категория имеет корректный mcId и keyPhrases")
    void whenLoaded_thenEachCategoryHasValidFields() {
        MicrocategoryLoaderService service = new MicrocategoryLoaderService();
        service.loadDictionary();

        for (Microcategory mc : service.getDictionary()) {
            assertTrue(mc.mcId() > 0, "mcId должен быть положительным: " + mc.mcId());
            assertNotNull(mc.mcTitle(), "mcTitle не должен быть null");
            assertFalse(mc.mcTitle().isBlank(), "mcTitle не должен быть пустым");
            assertNotNull(mc.keyPhrases(), "keyPhrases не должен быть null");
            assertFalse(mc.keyPhrases().isEmpty(), "keyPhrases не должны быть пустыми для категории: " + mc.mcTitle());
        }
    }

    @Test
    @DisplayName("Словарь иммутабелен после загрузки")
    void whenLoaded_thenDictionaryIsUnmodifiable() {
        MicrocategoryLoaderService service = new MicrocategoryLoaderService();
        service.loadDictionary();

        List<Microcategory> dictionary = service.getDictionary();

        assertThrows(UnsupportedOperationException.class,
                () -> dictionary.add(new Microcategory(9999, "test", List.of("test"))),
                "Словарь должен быть неизменяемым (unmodifiable)");
    }

    @Test
    @DisplayName("getDictionary возвращает пустой список до загрузки")
    void whenNotLoaded_thenDictionaryIsEmpty() {
        MicrocategoryLoaderService service = new MicrocategoryLoaderService();
        // Не вызываем loadDictionary()
        assertTrue(service.getDictionary().isEmpty(), "До загрузки словарь должен быть пустым");
    }
}
