package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable // @IdClass 대신 @Embeddable을 사용합니다.
public class SummaryArchiveTagId implements Serializable {

    private Long summaryArchiveId;
    private Long tagId;
}