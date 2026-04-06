package ru.avito.hackathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Микрокатегория со списком ключевых фраз для поиска в тексте.
 */
@Schema(description = "Структура словарной микрокатегории Авито с ключевыми фразами")
public record Microcategory(
        @Schema(description = "ID микрокатегории", example = "101")
        @Positive(message = "mcId должен быть больше 0")
        int mcId,
        
        @Schema(description = "Читаемое название категории", example = "Сантехника")
        @NotBlank(message = "mcTitle не может быть пустым")
        String mcTitle,
        
        @Schema(description = "Перечень характерных ключевых фраз, описывающих микрокатегорию", example = "[\"установка сантехники\", \"разводка труб\"]")
        @NotEmpty(message = "Список ключевых фраз не может быть пустым")
        List<@NotBlank(message = "Ключевая фраза не может быть пустой") String> keyPhrases
) {
}
