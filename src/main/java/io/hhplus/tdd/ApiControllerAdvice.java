package io.hhplus.tdd;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }

    //UserValidate에서 던진 예외를 가로채서 ErrorResponse방식으로 변환해서 반환
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        
        String errorCode = "400";   //임의
        String errorMessage = e.getMessage();

        ErrorResponse errorResponse = new ErrorResponse(errorCode, errorMessage);
        return ResponseEntity.status(400).body(errorResponse);
    }
}
