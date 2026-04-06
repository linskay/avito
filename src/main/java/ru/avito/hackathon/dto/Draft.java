package ru.avito.hackathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Черновик нового самостоятельного объявления, сгенерированного алгоритмом")
public record Draft(
        @Schema(description = "ID микрокатегории, к которой принадлежит новая услуга", example = "101")
        int mcId,
        
        @Schema(description = "Название микрокатегории выделенной услуги", example = "Сантехника")
        String mcTitle,
        
        @Schema(description = "Текст, который будет автоматически предложен пользователю для нового черновика (предложение из оригинального текста, где упоминается эта услуга отдельно)", example = "А также отдельно выполняем сантехнику.")
        String text
) {
}
