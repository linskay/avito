package ru.avito.hackathon.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import ru.avito.hackathon.dto.Microcategory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис динамической загрузки справочника микрокатегорий.
 * <p>
 * При запуске приложения считывает CSV-файл из ресурсов и формирует
 * справочник для эвристического анализа. Позволяет обновлять список
 * доступных категорий и их ключевых фраз без изменения программного кода.
 * <p>
 * Структура CSV:
 * {@code mcId,mcTitle,"фраза1; фраза2",описание}
 */
@Service
public class MicrocategoryLoaderService {

    private static final Logger LOG = Logger.getLogger(MicrocategoryLoaderService.class.getName());

    /** Путь к файлу словаря в classpath */
    private static final String CSV_RESOURCE_PATH = "/microcategories.csv";

    /** Разделитель ключевых фраз внутри колонки keyPhrases */
    private static final String PHRASE_SEPARATOR = ";";

    private List<Microcategory> dictionary = Collections.emptyList();

    /**
     * Загружает словарь из CSV при создании бина.
     * Если файл не найден или повреждён, оставляет пустой словарь и пишет предупреждение.
     */
    @PostConstruct
    public void loadDictionary() {
        List<Microcategory> loaded = new ArrayList<>();

        try (InputStream is = MicrocategoryLoaderService.class.getResourceAsStream(CSV_RESOURCE_PATH)) {
            if (is == null) {
                LOG.warning("Файл словаря не найден в classpath: " + CSV_RESOURCE_PATH + ". Алгоритм будет работать с пустым словарём.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Пропускаем заголовок
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    if (line.isBlank()) continue;

                    Microcategory mc = parseLine(line);
                    if (mc != null) {
                        loaded.add(mc);
                    }
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Ошибка при чтении файла словаря микрокатегорий", e);
        }

        this.dictionary = Collections.unmodifiableList(loaded);
        LOG.info("Словарь микрокатегорий загружен успешно: " + this.dictionary.size() + " категорий.");
    }

    /**
     * Возвращает загруженный неизменяемый список микрокатегорий.
     *
     * @return список {@link Microcategory}
     */
    public List<Microcategory> getDictionary() {
        return dictionary;
    }

    /**
     * Разбирает строку CSV в объект {@link Microcategory}.
     * <p>
     * Ожидаемый формат: {@code mcId,mcTitle,"phrase1; phrase2; ...",description}
     *
     * @param line строка из CSV-файла
     * @return объект {@link Microcategory} или {@code null} при ошибке разбора
     */
    private Microcategory parseLine(String line) {
        try {
            // Разбираем с учётом возможных CSV-кавычек вокруг keyPhrases-колонки
            String[] parts = splitCsvLine(line);
            if (parts.length < 3) return null;

            int mcId = Integer.parseInt(parts[0].trim());
            String mcTitle = parts[1].trim();
            // Убираем внешние кавычки, если они есть
            String rawPhrases = parts[2].trim().replaceAll("^\"|\"$", "");

            List<String> keyPhrases = Arrays.stream(rawPhrases.split(PHRASE_SEPARATOR))
                    .map(String::trim)
                    .filter(p -> !p.isBlank())
                    .toList();

            return new Microcategory(mcId, mcTitle, keyPhrases);

        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            LOG.warning("Ошибка разбора строки CSV: [" + line + "] — " + e.getMessage());
            return null;
        }
    }

    /**
     * Разбивает строку CSV по запятой, учитывая поля в двойных кавычках.
     *
     * @param line строка CSV
     * @return массив полей
     */
    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString()); // последний элемент
        return result.toArray(new String[0]);
    }
}
