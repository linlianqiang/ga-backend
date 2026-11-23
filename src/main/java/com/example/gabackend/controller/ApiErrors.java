package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) public ResponseEntity<?> bad(Exception e){return ResponseEntity.badRequest().body(Result.error(400,e instanceof IllegalArgumentException?e.getMessage():"请求格式无效"));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Result.error(409,"数据重复或仍存在关联"));}
}
