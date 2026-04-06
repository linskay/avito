package ru.avito.hackathon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.avito.hackathon.dto.Draft;
import ru.avito.hackathon.entity.DraftEntity;

/**
 * MapStruct-маппер для преобразования между {@link Draft} DTO и {@link DraftEntity} сущностью.
 * <p>
 * Поля {@code id} и {@code adId} в сущности не заполняются маппером:
 * {@code id} генерируется базой данных, {@code adId} устанавливается вручную
 * в {@code AdSplitterFacade} после сохранения родительского объявления.
 */
@Mapper(componentModel = "spring")
public interface DraftMapper {

    /**
     * Преобразует DTO черновика в сущность для сохранения в базу данных.
     * Поля {@code id} и {@code adId} явно игнорируются — они задаются вне маппера.
     *
     * @param dto черновик из результата анализа
     * @return сущность для сохранения
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "adId", ignore = true)
    DraftEntity toEntity(Draft dto);

    /**
     * Преобразует сущность черновика из базы данных в DTO.
     *
     * @param entity сущность из репозитория
     * @return DTO черновика
     */
    Draft toDto(DraftEntity entity);
}
