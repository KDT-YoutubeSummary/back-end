package com.kdt.yts.YouSumback.model.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class SummaryArchiveTagId implements Serializable {

    private Long summaryArchiveId;  // SummaryArchive의 ID 필드명과 일치
    private Long tagId;             // Tag의 ID 필드명과 일치

    // 기본 생성자
    public SummaryArchiveTagId() {
    }

    // 생성자
    public SummaryArchiveTagId(Long summaryArchiveId, Long tagId) {
        this.summaryArchiveId = summaryArchiveId;
        this.tagId = tagId;
    }
}
