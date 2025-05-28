package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserLibrarySaveRequestDTO {
    private int userId;
    private int summaryId;
    private List<String> tags;
    private String userNotes;
}
