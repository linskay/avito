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
 */
@Service
public class DependencyTraversalService {

    /**
     * Маркеры обособленности — слова и фразы, которые в тексте объявления
     * указывают на то, что конкретная услуга предлагается самостоятельно,
     * а не только в составе комплексного ремонта.
     */
    private static final Set<String> INDEPENDENCE_MARKERS = Set.of(
            "отдельно", "дополнительно", "самостоятельно", "также", "прайс", "цены", "раздельно", "по отдельности"
    );

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
        Set<Integer> detectedMcIdsSet = new HashSet<>();

        String[] tokens = desc.split("[.!?;\\n]|\\s[,/|+]\\s|[,/|\\n]|\\s+и\\s+");
        
        for (String originalToken : tokens) {
            String tokenTrimmed = originalToken.trim();
            if (tokenTrimmed.length() < 3) continue;
            String lowerToken = tokenTrimmed.toLowerCase();

            for (Microcategory mc : dictionary) {
                boolean matches = mc.keyPhrases().stream()
                        .anyMatch(phrase -> lowerToken.contains(phrase.toLowerCase()));

                if (matches) {
                    detectedMcIdsSet.add(mc.mcId());
                    
                    if (mc.mcId() != ad.mcId() && !uniqueMcIds.contains(mc.mcId())) {
                        String cleanToken = tokenTrimmed.replaceAll("^[-•,✔*\\s—]+", "");
                        discoveredDrafts.add(new Draft(mc.mcId(), mc.mcTitle(), cleanToken));
                        uniqueMcIds.add(mc.mcId());
                    }
                }
            }
        }

        boolean isHardTurnkey = HARD_TURNKEY_STOPPERS.stream().anyMatch(lowerDesc::contains);
        
        boolean hasIndependenceMarker = INDEPENDENCE_MARKERS.stream()
                .anyMatch(lowerDesc::contains);

        boolean shouldSplit = false;
        int size = discoveredDrafts.size();

        if (size > 0) {
            if (hasIndependenceMarker) {
                shouldSplit = true;
            } else if (size >= 2) {
                shouldSplit = true;
            } else {
                shouldSplit = !isHardTurnkey;
            }
        }

        if (shouldSplit) {
            return new SplitResult(new ArrayList<>(detectedMcIdsSet), true, discoveredDrafts);
        }

        return new SplitResult(new ArrayList<>(detectedMcIdsSet), false, List.of());
    }

}
