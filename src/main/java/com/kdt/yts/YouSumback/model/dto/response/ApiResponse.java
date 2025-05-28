package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
// ApiResponse는 API 응답의 표준 형식을 정의하는 클래스
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static ApiResponse<List<UserLibraryResponseDTO>> success(int i, String library_read_success, List<UserLibraryResponseDTO> libraryList) {
        return new ApiResponse<>(i, library_read_success, libraryList);
    }
}
