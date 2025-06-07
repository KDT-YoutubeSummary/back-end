package com.kdt.yts.YouSumback.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.dto.response.UserActivityLogDTO;
import com.kdt.yts.YouSumback.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService{

    private final UserActivityLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public List<UserActivityLogDTO> getRecentLogs(Long userId) {
        return logRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(log -> UserActivityLogDTO.builder()
                        .activityType(log.getActivityType())
                        .targetEntityType(log.getTargetEntityType())
                        .targetEntityId(
                                log.getTargetEntityIdStr() != null
                                        ? Long.valueOf(log.getTargetEntityIdStr())
                                        : log.getTargetEntityIdInt()
                        )
                        .createdAt(log.getCreatedAt())
                        .activityDetail(log.getActivityDetail())
                        .details(convertJsonToMap(log.getDetails()))
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of(); // fallback to empty map
        }
    }
}
