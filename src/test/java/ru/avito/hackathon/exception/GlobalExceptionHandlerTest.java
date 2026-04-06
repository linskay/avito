package ru.avito.hackathon.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.avito.hackathon.api.AdSplitter;
import ru.avito.hackathon.dto.SplitResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для {@link GlobalExceptionHandler}.
 * Проверяет, что API возвращает стандартизированный JSON при ошибках валидации.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Тесты GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdSplitter adSplitter;

    @Test
    @DisplayName("Пустое тело запроса → 400 с errorCode VALIDATION_ERROR")
    void whenBodyIsEmpty_thenReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Корректный запрос → 200 со структурой SplitResult")
    void whenValidRequest_thenReturns200() throws Exception {
        when(adSplitter.splitAd(any(), any()))
                .thenReturn(new SplitResult(List.of(), false, List.of()));

        String validBody = """
                {
                  "ad": {
                    "itemId": 1,
                    "mcId": 201,
                    "mcTitle": "Ремонт",
                    "description": "Делаем ремонт под ключ."
                  },
                  "dictionary": [
                    {
                      "mcId": 101,
                      "mcTitle": "Сантехника",
                      "keyPhrases": ["разводка труб"]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldSplit").value(false))
                .andExpect(jsonPath("$.drafts").isArray());
    }

    @Test
    @DisplayName("Пустое поле description → 400 с упоминанием поля")
    void whenDescriptionIsBlank_thenReturns400WithFieldName() throws Exception {
        String body = """
                {
                  "ad": {
                    "itemId": 1,
                    "mcId": 201,
                    "mcTitle": "Ремонт",
                    "description": ""
                  },
                  "dictionary": [
                    {
                      "mcId": 101,
                      "mcTitle": "Сантехника",
                      "keyPhrases": ["разводка труб"]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("description")));
    }

    @Test
    @DisplayName("Пустой список dictionary → 400 Bad Request")
    void whenDictionaryIsEmpty_thenReturns400() throws Exception {
        String body = """
                {
                  "ad": {
                    "itemId": 1,
                    "mcId": 201,
                    "mcTitle": "Ремонт",
                    "description": "Текст объявления"
                  },
                  "dictionary": []
                }
                """;

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
