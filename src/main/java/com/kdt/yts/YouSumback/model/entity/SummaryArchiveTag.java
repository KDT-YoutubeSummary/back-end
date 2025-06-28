package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "summary_archive_tag")
@IdClass(SummaryArchiveTagId.class)
public class SummaryArchiveTag {

    @Id
    @Column(name = "archive_id")
    private Long summaryArchiveId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", insertable = false, updatable = false)
    private SummaryArchive summaryArchive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private Tag tag;

    // 기본 생성자
    public SummaryArchiveTag() {
    }

    // SummaryArchive와 Tag를 이용한 생성자
    public SummaryArchiveTag(SummaryArchive summaryArchive, Tag tag) {
        this.summaryArchive = summaryArchive;
        this.tag = tag;
        this.summaryArchiveId = summaryArchive.getId();
        this.tagId = tag.getId();
    }

    // ID만으로 생성하는 생성자 추가
    public SummaryArchiveTag(Long summaryArchiveId, Long tagId) {
        this.summaryArchiveId = summaryArchiveId;
        this.tagId = tagId;
    }
}