package ru.avito.hackathon.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("ad")
public class AdEntity {
    @Id
    private Long id;
    private Integer itemId;
    private Integer mcId;
    private String mcTitle;
    private String description;
}
