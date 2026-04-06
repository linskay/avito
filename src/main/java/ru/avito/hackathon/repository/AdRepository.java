package ru.avito.hackathon.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.avito.hackathon.entity.AdEntity;

@Repository
public interface AdRepository extends CrudRepository<AdEntity, Long> {
}
