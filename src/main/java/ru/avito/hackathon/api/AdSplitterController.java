package ru.avito.hackathon.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.avito.hackathon.dto.AdSplitRequest;
import ru.avito.hackathon.dto.SplitResult;

@Tag(name = "Анализатор объявлений", description = "Автоматическое выделение составных услуг в отдельные черновики с использованием легковесного эвристического алгоритма")
@RestController
@RequestMapping("/api/v1/ads")
public class AdSplitterController {

    private final AdSplitter adSplitter;

    public AdSplitterController(AdSplitter adSplitter) {
        this.adSplitter = adSplitter;
    }

    @Operation(
            summary = "Анализировать объявление (без сохранения в БД)",
            description = "Анализирует текст объявления и возвращает список найденных микрокатегорий и возможных черновиков. Результат НЕ сохраняется в базу данных. Используется для проверки качества алгоритма и отладки."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный анализ"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные",
                    content = @Content(schema = @Schema(implementation = ru.avito.hackathon.exception.ErrorResponse.class)))
    })
    @PostMapping("/analyze")
    public SplitResult analyzeAd(@Valid @RequestBody AdSplitRequest request) {
        return adSplitter.analyzeAd(request.ad(), request.dictionary());
    }

    @Operation(
            summary = "Разделить объявление на самостоятельные услуги (с сохранением в БД)",
            description = "Анализирует текст объявления и при нахождении самостоятельных услуг генерирует черновики. Объявление и черновики сохраняются в PostgreSQL."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный анализ объявления"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные",
                    content = @Content(schema = @Schema(implementation = ru.avito.hackathon.exception.ErrorResponse.class)))
    })
    @PostMapping("/split")
    public SplitResult splitAd(@Valid @RequestBody AdSplitRequest request) {
        return adSplitter.splitAd(request.ad(), request.dictionary());
    }
}
