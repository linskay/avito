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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для {@link GlobalExceptionHandler}.
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
    @DisplayName("POST /analyze — анализ без сохранения в БД, возвращает 200")
    void whenAnalyzeValidRequest_thenReturns200WithoutDbSave() throws Exception {
        when(adSplitter.analyzeAd(any(), any()))
                .thenReturn(new SplitResult(List.of(101), true, List.of()));

        mockMvc.perform(post("/api/v1/ads/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldSplit").value(true))
                .andExpect(jsonPath("$.detectedMcIds[0]").value(101));
    }

    @Test
    @DisplayName("Пустое тело запроса (поле ad=null) → 400 VALIDATION_ERROR")
    void whenAdIsNull_thenReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Корректный запрос → 200 OK с SplitResult")
    void whenValidRequest_thenReturns200() throws Exception {
        when(adSplitter.splitAd(any(), any()))
                .thenReturn(new SplitResult(List.of(), false, List.of()));

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldSplit").value(false))
                .andExpect(jsonPath("$.drafts").isArray());
    }

    @Test
    @DisplayName("Пустое поле description → 400 с именем поля в сообщении")
    void whenDescriptionIsBlank_thenReturns400WithFieldName() throws Exception {
        String body = """
                {
                  "ad": {
                    "itemId": 1, "mcId": 201, "mcTitle": "Ремонт",
                    "description": ""
                  },
                  "dictionary": [{ "mcId": 101, "mcTitle": "Сантехника", "keyPhrases": ["разводка труб"] }]
                }
                """;

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("description")));
    }

    @Test
    @DisplayName("Пустой словарь dictionary=[] → 400 Bad Request")
    void whenDictionaryIsEmpty_thenReturns400() throws Exception {
        String body = """
                {
                  "ad": { "itemId": 1, "mcId": 201, "mcTitle": "Ремонт", "description": "Текст" },
                  "dictionary": []
                }
                """;

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Сервис бросает RuntimeException → 500 с errorCode INTERNAL_SERVER_ERROR")
    void whenServiceThrows_thenReturns500() throws Exception {
        when(adSplitter.splitAd(any(), any()))
                .thenThrow(new RuntimeException("Database is down"));

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("shouldSplit=true в ответе когда сервис вернул черновики")
    void whenServiceReturnsDrafts_thenShouldSplitIsTrue() throws Exception {
        ru.avito.hackathon.dto.Draft draft = new ru.avito.hackathon.dto.Draft(101, "Сантехника", "разводка труб");
        when(adSplitter.splitAd(any(), any()))
                .thenReturn(new SplitResult(List.of(101), true, List.of(draft)));

        mockMvc.perform(post("/api/v1/ads/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldSplit").value(true))
                .andExpect(jsonPath("$.drafts[0].mcId").value(101))
                .andExpect(jsonPath("$.drafts[0].mcTitle").value("Сантехника"))
                .andExpect(jsonPath("$.detectedMcIds[0]").value(101));
    }

    private String validBody() {
        return """
                {
                  "ad": {
                    "itemId": 1,
                    "mcId": 201,
                    "mcTitle": "Ремонт",
                    "description": "Делаем ремонт под ключ."
                  },
                  "dictionary": [
                    { "mcId": 101, "mcTitle": "Сантехника", "keyPhrases": ["разводка труб"] }
                  ]
                }
                """;
    }
}
