// TagStatDTO.java
package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TagStatResponseDTO {
    private String tag;
    private Long count;
}
