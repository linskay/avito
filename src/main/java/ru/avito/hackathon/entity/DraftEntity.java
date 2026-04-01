package ru.avito.hackathon.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("draft")
public class DraftEntity {
    @Id
    private Long id;
    private Long adId;
    private Integer mcId;
    private String mcTitle;
    private String text;
}
