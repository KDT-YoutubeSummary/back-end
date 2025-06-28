package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "summary_archive_tag")
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}