package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable // @IdClass 대신 @Embeddable을 사용합니다.
public class SummaryArchiveTagId implements Serializable {

    @Column(name = "archive_id")
    private Long summaryArchiveId;
    @Column(name = "tag_id")
    private Long tagId;
}