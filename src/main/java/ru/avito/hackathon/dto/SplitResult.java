package ru.avito.hackathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Финальный результат анализа объявления")
public record SplitResult(
        @Schema(description = "Флаг, указывающий, нужно ли предлагать пользователю создать отдельные черновики", example = "true")
        boolean shouldSplit,
        
        @Schema(description = "Список предложенных алгоритмом новых черновиков для обособленных услуг")
        List<Draft> drafts
) {
}
