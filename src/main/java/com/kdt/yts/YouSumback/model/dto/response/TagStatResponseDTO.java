// TagStatDTO.java
package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// TagStatResponseDTO는 태그 통계 정보를 담는 DTO 클래스입니다.
public class TagStatResponseDTO {
    private String tag;
    private Long count;
}
