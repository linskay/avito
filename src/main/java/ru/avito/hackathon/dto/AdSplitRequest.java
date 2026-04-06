package ru.avito.hackathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Запрос на разделение объявления")
public record AdSplitRequest(
        @Schema(description = "Исходное объявление пользователя")
        @NotNull(message = "Объявление (ad) обязательно")
        @Valid
        Ad ad,
        
        @Schema(description = "Словарь микрокатегорий для поиска")
        @NotEmpty(message = "Словарь микрокатегорий не должен быть пустым")
        @Valid
        List<Microcategory> dictionary
) {
}
