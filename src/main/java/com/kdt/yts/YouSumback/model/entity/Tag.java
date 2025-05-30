package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Tag")
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // tag_id가 DB에서 자동 증가하도록 설정
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "tag_name", length = 100, nullable = false, unique = true)
    // tag_name은 고유하고 무조건 존재
    private String tagName;

    // tag_name만 받는 생성자
    public Tag(String tagName) {
        this.tagName = tagName;
    }

}
