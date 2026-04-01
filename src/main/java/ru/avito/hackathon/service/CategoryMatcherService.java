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

        // Разбиваем текст на предложения для более точного анализа контекста
        String[] sentences = splitIntoSentences(text);

        for (String sentence : sentences) {
            String lowerCaseSentence = sentence.toLowerCase();

            for (Microcategory mc : dictionary) {
                for (String phrase : mc.keyPhrases()) {
                    if (lowerCaseSentence.contains(phrase.toLowerCase())) {
                        found.add(new FoundMicrocategory(mc, phrase, sentence.trim()));
                        // Если одна фраза из микрокатегории найдена в предложении,
                        // этого достаточно для привязки этой категории к предложению
                        break;
                    }
                }
            }
        }

        return found;
    }

    /**
     * Упрощенное разбиение текста на предложения.
     * Учитывает точки, восклицательные и вопросительные знаки.
     */
    private String[] splitIntoSentences(String text) {
        // Разделяем по точкам, восклицательным и вопросительным знакам, 
        // за которыми следует пробел или конец строки.
        return text.split("(?<=[.!?])\\s+|(?<=[.!?])$");
    }
}
