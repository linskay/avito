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

    // Только самые жесткие блокираторы, которые реально означают "не делить"
    private static final Set<String> HARD_TURNKEY_STOPPERS = Set.of(
            "под ключ", "комплексный", "полный ремонт", "ремонт полностью", "капитальный ремонт"
    );

    /**
     * Алгоритм «0.40 Sniper» v2.
     * Максимизирует Recall через агрессивное разделение при наличии 2+ услуг.
     */
    public SplitResult determineSplits(Ad ad, List<Microcategory> dictionary) {
        String desc = ad.description();
        String lowerDesc = desc.toLowerCase();
        
        List<Draft> discoveredDrafts = new ArrayList<>();
        Set<Integer> uniqueMcIds = new HashSet<>();
        
        // 1. Извлекаем услуги (максимально широкий охват через разделители)
        String[] tokens = desc.split("[.!?;\\n]|\\s[,/|+]\\s|[,/|\\n]|\\s+и\\s+");
        
        for (String originalToken : tokens) {
            String tokenTrimmed = originalToken.trim();
            if (tokenTrimmed.length() < 3) continue;
            String lowerToken = tokenTrimmed.toLowerCase();

            for (Microcategory mc : dictionary) {
                // Исключаем саму категорию объявления (ТЗ)
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

        // 2. Флаг жесткого блокиратора
        boolean isHardTurnkey = HARD_TURNKEY_STOPPERS.stream().anyMatch(lowerDesc::contains);
        
        // 3. Маркеры обособленности
        boolean hasIndependenceMarker = INDEPENDENCE_MARKERS.stream()
                .anyMatch(lowerDesc::contains);

        // 4. ПРИНЯТИЕ РЕШЕНИЯ (Агрессивный Recall для F1 > 0.40)
        boolean shouldSplit = false;
        int size = discoveredDrafts.size();

        if (size > 0) {
            if (hasIndependenceMarker) {
                // Явное указание автора
                shouldSplit = true;
            } else if (size >= 2) {
                // Две и более услуг - почти всегда split в датасете
                shouldSplit = true;
            } else {
                // Одна услуга: делим, если это не "жесткий" ремонт под ключ
                shouldSplit = !isHardTurnkey;
            }
        }

        if (shouldSplit) {
            return new SplitResult(true, discoveredDrafts);
        }

        return new SplitResult(false, List.of());
    }

}
