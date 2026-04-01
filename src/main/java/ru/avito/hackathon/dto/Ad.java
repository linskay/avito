package ru.avito.hackathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Данные исходного объявления Авито.
 */
@Schema(description = "Данные исходного объявления Авито")
public record Ad(
        @Schema(description = "Внутренний ID объявления Авито", example = "5001")
        int itemId,
        
        @Schema(description = "ID исходной микрокатегории, к которой относится объявление", example = "201")
        int mcId,
        
        @Schema(description = "Название исходной микрокатегории", example = "Ремонт квартир под ключ")
        String mcTitle,
        
        @Schema(description = "Полный текст пользователя с описанием услуг", example = "Делаем ремонт квартир под ключ. А также отдельно выполняем сантехнику.")
        String description
) {
}
