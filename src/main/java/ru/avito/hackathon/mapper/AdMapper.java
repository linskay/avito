package ru.avito.hackathon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.avito.hackathon.dto.Ad;
import ru.avito.hackathon.entity.AdEntity;

/**
 * MapStruct-маппер для преобразования между {@link Ad} DTO и {@link AdEntity} сущностью.
 * <p>
 * Поле {@code id} сущности игнорируется при маппинге из DTO:
 * оно генерируется базой данных автоматически при сохранении.
 */
@Mapper(componentModel = "spring")
public interface AdMapper {

    /**
     * Преобразует DTO объявления в сущность для сохранения в базу данных.
     * Поле {@code id} явно игнорируется — оно задаётся базой данных.
     *
     * @param dto данные объявления из запроса
     * @return сущность для сохранения
     */
    @Mapping(target = "id", ignore = true)
    AdEntity toEntity(Ad dto);

    /**
     * Преобразует сущность объявления из базы данных в DTO.
     *
     * @param entity сущность из репозитория
     * @return DTO объявления
     */
    Ad toDto(AdEntity entity);
}
