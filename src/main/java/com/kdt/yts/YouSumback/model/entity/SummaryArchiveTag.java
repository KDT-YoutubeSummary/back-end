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

    @EmbeddedId
    private SummaryArchiveTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("summaryArchiveId") // SummaryArchiveTagId의 summaryArchiveId 필드에 매핑
    @JoinColumn(name = "summary_archive_id")
    private SummaryArchive summaryArchive;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId") // SummaryArchiveTagId의 tagId 필드에 매핑
    @JoinColumn(name = "tag_id")
    private Tag tag;
}