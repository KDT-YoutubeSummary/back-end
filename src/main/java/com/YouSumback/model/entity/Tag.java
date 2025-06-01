package com.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Tag {

    @Id
    @Column(name = "tag_id", nullable = false)
    private int tagId;

    @Column(name = "tag_name", length = 100, nullable = true)
    private String tagName;
}
