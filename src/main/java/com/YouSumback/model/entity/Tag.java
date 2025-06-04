package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", nullable = false)
    private Long tagId;  // ✅ 카멜케이스 + Long 타입 변경

    @Column(name = "tag_name", length = 100, nullable = false, unique = true)
    private String tagName; // ✅ 카멜케이스로 수정
}
