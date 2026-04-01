package ru.avito.hackathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Микрокатегория со списком ключевых фраз для поиска в тексте.
 */
@Schema(description = "Структура словарной микрокатегории Авито с ключевыми фразами")
public record Microcategory(
        @Schema(description = "ID микрокатегории", example = "101")
        int mcId,
        
        @Schema(description = "Читаемое название категории", example = "Сантехника")
        String mcTitle,
        
        @Schema(description = "Перечень характерных ключевых фраз, описывающих микрокатегорию", example = "[\"установка сантехники\", \"разводка труб\"]")
        List<String> keyPhrases
) {
}
