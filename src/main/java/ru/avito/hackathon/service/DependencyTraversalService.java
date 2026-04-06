package ru.avito.hackathon.service;

import org.springframework.stereotype.Service;
import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.dto.Draft;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.dto.SplitResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис эвристического анализа текста объявления для выделения самостоятельных услуг.
 * <p>
 * Принцип работы:
 * <ol>
 *   <li>Разбитие на токены: Описание объявления делится на предложения и фрагменты по знакам препинания,
 *       переносам строк и разделителям «/», «+» (характерно для коротких списков услуг).</li>
 *   <li>Поиск маркеров обособленности: В каждом фрагменте ищется фраза-маркер (например: «отдельно»,
 *       «в качестве самостоятельной услуги»), указывающая на то, что работа не является частью комплекса.</li>
 *   <li>Сопоставление со словарем: Если маркер найден, фрагмент анализируется на наличие ключевых фраз
 *       микрокатегорий из динамического справочника.</li>
 *   <li>Формирование черновиков: При совпадении создается объект {@link Draft}, где в качестве текста
 *       используется исходный фрагмент (сохраняя контекст автора).</li>
 *   <li>Дедупликация: Алгоритм следит, чтобы для одной микрокатегории не создавалось более одного черновика,
 *       и исключает исходную категорию объявления.</li>
 * </ol>
 */
@Service
public class DependencyTraversalService {

    /**
     * Маркеры обособленности — слова и фразы, которые в тексте объявления
     * указывают на то, что конкретная услуга предлагается самостоятельно,
     * а не только в составе комплексного ремонта.
     * <p>
     * Расширенный список составлен на основе анализа 3000 объявлений из датасета
     * (типы: turnkey_split, multi_service_split, bullets_mixed).
     */
    private static final Set<String> INDEPENDENCE_MARKERS = Set.of(
            // Базовые маркеры
            "отдельно",
            "дополнительно",
            "самостоятельн",
            // Фразовые маркеры из bullets_mixed формата
            "беру как самостоятельную",
            "как самостоятельную",
            "как отдельную услугу",
            "можно заказать отдельно",
            "беру как самостоятельн",
            // Глагольные маркеры из multi_service_split и turnkey_split
            "также отдельно",
            "делаем отдельно",
            "выполняем отдельно",
            "берем отдельно",
            "можем отдельно",
            "при необходимости отдельно",
            "делаю отдельно",
            "берем как отдельн",
            "выполняем как отдельн"
    );

    /**
     * Анализирует текст объявления и формирует список черновиков для найденных
     * независимых услуг.
     * <p>
     * Метод корректно обрабатывает разные форматы текстов: структурированные
     * (bullet-list), неструктурированные (plain text) и «шумные» (noisy_short).
     * Дедупликация результатов выполняется по идентификатору микрокатегории {@code mcId}.
     *
     * @param ad         исходное объявление с текстом и идентификатором исходной категории
     * @param dictionary словарь микрокатегорий с ключевыми фразами для поиска
     * @return {@link SplitResult} — результат анализа: список черновиков и флаг разделения
     */
    public SplitResult determineSplits(Ad ad, List<Microcategory> dictionary) {
        String desc = ad.description(); // сохраняем оригинальный регистр
        List<Draft> discoveredDrafts = new ArrayList<>();

        // Множество уже найденных mcId для дедупликации черновиков
        Set<Integer> foundMcIds = new HashSet<>();
        // Исходную категорию объявления не включаем в черновики
        foundMcIds.add(ad.mcId());
        
        Set<Integer> detectedMcIdsSet = new HashSet<>();

        // Разбиваем на токены по: знакам препинания, переносам строк,
        // а также по разделителям «/» и «+» (формат noisy_short в датасете)
        String[] tokens = desc.split("[.!?;\\n]|\\s*[/+]\\s*");

        for (String originalToken : tokens) {
            if (originalToken.isBlank()) continue;

            String lowerToken = originalToken.toLowerCase();

            // Проверяем наличие маркера обособленности
            boolean hasMarker = INDEPENDENCE_MARKERS.stream()
                    .anyMatch(lowerToken::contains);

            // Ищем совпадение с микрокатегорией из словаря
            for (Microcategory mc : dictionary) {
                boolean matches = mc.keyPhrases().stream()
                        .anyMatch(phrase -> lowerToken.contains(phrase.toLowerCase()));

                if (matches) {
                    detectedMcIdsSet.add(mc.mcId());
                    
                    if (hasMarker && !foundMcIds.contains(mc.mcId())) {
                        // Очищаем токен от ведущих символов-маркеров списков
                        String cleanToken = originalToken.trim().replaceAll("^[-•,\\s]+", "");
                        discoveredDrafts.add(new Draft(mc.mcId(), mc.mcTitle(), cleanToken));
                        foundMcIds.add(mc.mcId());
                    }
                }
            }
        }

        return new SplitResult(new ArrayList<>(detectedMcIdsSet), !discoveredDrafts.isEmpty(), discoveredDrafts);
    }
}
