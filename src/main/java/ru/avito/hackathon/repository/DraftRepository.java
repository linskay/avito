package ru.avito.hackathon.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.avito.hackathon.entity.DraftEntity;

@Repository
public interface DraftRepository extends CrudRepository<DraftEntity, Long> {
}
