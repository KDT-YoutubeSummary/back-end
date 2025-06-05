package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Tag {

    @Id
    @Column(name = "tag_id", nullable = false)
    private int tag_id;

    @Column(name = "tag_name", length = 100, nullable = true)
    private String tag_name;
}
