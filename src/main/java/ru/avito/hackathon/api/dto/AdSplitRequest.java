package ru.avito.hackathon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.dto.Microcategory;

import java.util.List;

@Schema(description = "Запрос на разделение объявления")
public record AdSplitRequest(
        @Schema(description = "Исходное объявление пользователя")
        Ad ad,
        @Schema(description = "Словарь микрокатегорий для поиска (обычно подгружается из другой микро-сервисной системы Авито)")
        List<Microcategory> dictionary
) {
}
