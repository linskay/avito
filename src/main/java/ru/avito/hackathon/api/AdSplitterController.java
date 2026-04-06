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
            summary = "Разделить объявление на самостоятельные услуги",
            description = "Принимает текст объявления и словарь микрокатегорий. Анализирует структуру текста с помощью легковесного токенизатора..."
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
