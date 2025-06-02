package com.YouSumback.exception;

import org.springframework.http.HttpStatus; // HTTP 상태 코드
import org.springframework.web.bind.annotation.ResponseStatus; // HTTP 응답 상태 어노테이션

@ResponseStatus(HttpStatus.NOT_FOUND) // 이 예외가 발생하면 HTTP 404 Not Found 상태 코드를 반환합니다.
public class ResourceNotFoundException extends RuntimeException {
    // 리소스를 찾을 수 없을 때 발생하는 예외 클래스입니다.

    public ResourceNotFoundException(String message) {
        // 예외 메시지를 인자로 받아 부모 클래스인 RuntimeException의 생성자를 호출합니다.
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        // 메시지와 원인 예외를 함께 받아 부모 클래스의 생성자를 호출합니다.
        super(message, cause);
    }
}