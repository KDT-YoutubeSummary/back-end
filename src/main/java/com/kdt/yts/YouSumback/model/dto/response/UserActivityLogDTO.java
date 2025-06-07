package com.kdt.yts.YouSumback.model.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLogDTO {
    private String activityType;
    private String targetEntityType;
    private Long targetEntityId;
    private LocalDateTime createdAt;
    private String activityDetail;
    private Map<String, Object> details;

}
