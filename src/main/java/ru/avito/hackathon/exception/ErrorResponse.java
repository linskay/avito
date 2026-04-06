package ru.avito.hackathon.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Стандартный ответ с ошибкой")
public record ErrorResponse(
        @Schema(description = "Время возникновения ошибки")
        LocalDateTime timestamp,
        
        @Schema(description = "HTTP статус")
        int status,
        
        @Schema(description = "Сообщение об ошибке (может содержать детали валидации)")
        String message,
        
        @Schema(description = "Уникальный код ошибки")
        String errorCode
) {
    public ErrorResponse(int status, String message, String errorCode) {
        this(LocalDateTime.now(), status, message, errorCode);
    }
}
