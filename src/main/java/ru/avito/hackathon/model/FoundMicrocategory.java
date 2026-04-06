package ru.avito.hackathon.model;

import ru.avito.hackathon.dto.Microcategory;

/**
 * Внутренняя структура данных для хранения информации о найденной микрокатегории в тексте.
 */
public record FoundMicrocategory(
        Microcategory microcategory,
        String matchedPhrase,
        String sentenceContext
) {
}
