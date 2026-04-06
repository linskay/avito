package ru.avito.hackathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Данные исходного объявления Авито.
 */
@Schema(description = "Данные исходного объявления Авито")
public record Ad(
        @Schema(description = "Внутренний ID объявления Авито", example = "5001")
        @Positive(message = "itemId должен быть больше 0")
        int itemId,
        
        @Schema(description = "ID исходной микрокатегории, к которой относится объявление", example = "201")
        @Positive(message = "mcId должен быть больше 0")
        int mcId,
        
        @Schema(description = "Название исходной микрокатегории", example = "Ремонт квартир под ключ")
        @NotBlank(message = "mcTitle не может быть пустым")
        String mcTitle,
        
        @Schema(description = "Полный текст пользователя с описанием услуг", example = "Делаем ремонт квартир под ключ. А также отдельно выполняем сантехнику.")
        @NotBlank(message = "description не может быть пустым")
        String description
) {
}
