package ru.avito.hackathon.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.dto.Draft;
import ru.avito.hackathon.dto.Microcategory;
import ru.avito.hackathon.dto.SplitResult;
import ru.avito.hackathon.entity.AdEntity;
import ru.avito.hackathon.entity.DraftEntity;
import ru.avito.hackathon.mapper.AdMapper;
import ru.avito.hackathon.mapper.DraftMapper;
import ru.avito.hackathon.repository.AdRepository;
import ru.avito.hackathon.repository.DraftRepository;
import ru.avito.hackathon.service.DependencyTraversalService;

import java.util.List;

/**
 * Фасад сервиса разделения объявлений.
 * <p>
 * Координирует процесс анализа текста, формирования черновиков в БД
 * и возврата результата клиенту. Является основной точкой входа бизнес-логики.
 */
@Service
public class AdSplitterFacade implements AdSplitter {

    private final DependencyTraversalService traversalService;
    private final AdRepository adRepository;
    private final DraftRepository draftRepository;
    private final AdMapper adMapper;
    private final DraftMapper draftMapper;

    public AdSplitterFacade(DependencyTraversalService traversalService,
                            AdRepository adRepository,
                            DraftRepository draftRepository,
                            AdMapper adMapper,
                            DraftMapper draftMapper) {
        this.traversalService = traversalService;
        this.adRepository = adRepository;
        this.draftRepository = draftRepository;
        this.adMapper = adMapper;
        this.draftMapper = draftMapper;
    }

    /**
     * Анализирует объявление, сохраняет его в БД и создает черновики для найденных услуг.
     *
     * @param ad         исходное объявление
     * @param dictionary справочник микрокатегорий
     * @return результат разделения с флагом shouldSplit и списком черновиков
     */
    @Override
    @Transactional
    public SplitResult splitAd(Ad ad, List<Microcategory> dictionary) {
        SplitResult result = traversalService.determineSplits(ad, dictionary);

        AdEntity adEntity = adMapper.toEntity(ad);
        adEntity.setId(null); 
        adEntity = adRepository.save(adEntity);

        if (result.shouldSplit() && !result.drafts().isEmpty()) {
            for (Draft draft : result.drafts()) {
                DraftEntity draftEntity = draftMapper.toEntity(draft);
                draftEntity.setId(null);
                draftEntity.setAdId(adEntity.getId());
                draftRepository.save(draftEntity);
            }
        }

        return result;
    }
}
