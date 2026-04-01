package ru.avito.hackathon.api;

import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.dto.SplitResult;

import java.util.List;

/**
 * Главный интерфейс решения для выделения самостоятельных услуг
 */
public interface AdSplitter {
    
    /**
     * Анализирует текст объявления и определяет, нужно ли создавать отдельные черновики.
     * 
     * @param ad Входное объявление
     * @param dictionary Словарь микрокатегорий
     * @return Результат с флагом shouldSplit и списком черновиков drafts
     */
    SplitResult splitAd(Ad ad, List<Microcategory> dictionary);
}
