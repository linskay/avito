package ru.avito.hackathon.api;

import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.dto.SplitResult;

import java.util.List;

/**
 * Контракт сервиса анализа и разделения объявлений.
 */
public interface AdSplitter {

    /**
     * Анализирует текст объявления без сохранения результата в БД.
     * Используется для проверки алгоритма и отладки.
     *
     * @param ad         исходное объявление
     * @param dictionary справочник микрокатегорий
     * @return результат анализа с флагом shouldSplit и списком черновиков
     */
    SplitResult analyzeAd(Ad ad, List<Microcategory> dictionary);

    /**
     * Анализирует текст объявления и сохраняет черновики в базу данных.
     *
     * @param ad         исходное объявление
     * @param dictionary справочник микрокатегорий
     * @return результат с флагом shouldSplit и списком сохранённых черновиков
     */
    SplitResult splitAd(Ad ad, List<Microcategory> dictionary);
}
