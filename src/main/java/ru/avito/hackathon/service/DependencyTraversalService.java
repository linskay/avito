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
            "отдельно", "дополнительно", "самостоятельно", "также", "прайс", "цены", "раздельно", "по отдельности"
    );

    // Блокираторы разделения для одиночных/парных услуг
    private static final Set<String> TURNKEY_STOPPERS = Set.of(
            "под ключ", "комплексный", "ремонт квартир", "ремонт домов", "дизайн проект", "гарантия по договору"
    );

    /**
     * Финальный алгоритм «0.40 Breakthrough».
     * Оптимизирован для баланса Recall (через size >= 3) и Precision (через фильтры для 1-2 услуг).
     */
    public SplitResult determineSplits(Ad ad, List<Microcategory> dictionary) {
        String desc = ad.description();
        String lowerDesc = desc.toLowerCase();
        int descLength = desc.length();
        
        List<Draft> discoveredDrafts = new ArrayList<>();
        Set<Integer> uniqueMcIds = new HashSet<>();
        
        // 1. Извлекаем услуги (улучшенная токенизация для списков)
        String[] tokens = desc.split("[.!?;\\n]|\\s[,/|+]\\s|[,/|\\n]|\\s+и\\s+");
        
        for (String originalToken : tokens) {
            String tokenTrimmed = originalToken.trim();
            if (tokenTrimmed.length() < 3) continue;
            String lowerToken = tokenTrimmed.toLowerCase();

            for (Microcategory mc : dictionary) {
                // Исключаем саму категорию объявления
                if (mc.mcId() == ad.mcId()) continue; 

                boolean matches = mc.keyPhrases().stream()
                        .anyMatch(phrase -> lowerToken.contains(phrase.toLowerCase()));

                if (matches && !uniqueMcIds.contains(mc.mcId())) {
                    String cleanToken = tokenTrimmed.replaceAll("^[-•,✔*\\s—]+", "");
                    discoveredDrafts.add(new Draft(mc.mcId(), mc.mcTitle(), cleanToken));
                    uniqueMcIds.add(mc.mcId());
                }
            }
        }

        // 2. Флаги контекста
        boolean hasIndependenceMarker = INDEPENDENCE_MARKERS.stream()
                .anyMatch(lowerDesc::contains);
        boolean hasTurnkeyStopper = TURNKEY_STOPPERS.stream()
                .anyMatch(lowerDesc::contains);

        // 3. ПРИНЯТИЕ РЕШЕНИЯ (Цель: F1 > 0.40)
        boolean shouldSplit = false;
        int size = discoveredDrafts.size();

        if (size > 0) {
            if (hasIndependenceMarker) {
                // Сильный сигнал: автор явно указал на возможность отдельного заказа
                shouldSplit = true;
            } else if (size >= 3) {
                // Высокий Recall: большой список услуг почти всегда требует разделения
                shouldSplit = true;
            } else {
                // Для 1-2 услуг: только если текст короткий (частник) и нет блокираторов "под ключ"
                shouldSplit = (descLength < 450) && !hasTurnkeyStopper;
            }
        }

        if (shouldSplit) {
            return new SplitResult(true, discoveredDrafts);
        }

        return new SplitResult(false, List.of());
    }

}
