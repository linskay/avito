package ru.avito.hackathon.service;

import org.springframework.stereotype.Service;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.model.FoundMicrocategory;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryMatcherService {

    /**
     * Поиск микрокатегорий в тексте по ключевым фразам.
     * Возвращает список совпадений с привязкой к предложению, где была найдена фраза.
     */
    public List<FoundMicrocategory> findMicrocategoriesInText(String text, List<Microcategory> dictionary) {
        List<FoundMicrocategory> found = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return found;
        }

        String[] sentences = splitIntoSentences(text);

        for (String sentence : sentences) {
            String lowerCaseSentence = sentence.toLowerCase();

            for (Microcategory mc : dictionary) {
                for (String phrase : mc.keyPhrases()) {
                    if (lowerCaseSentence.contains(phrase.toLowerCase())) {
                        found.add(new FoundMicrocategory(mc, phrase, sentence.trim()));
                        break;
                    }
                }
            }
        }

        return found;
    }

    private String[] splitIntoSentences(String text) {
        return text.split("(?<=[.!?])\\s+(?=[А-ЯЁA-Z])|\\n+");
    }
}
