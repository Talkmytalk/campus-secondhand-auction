package edu.campus.auction.web;

import edu.campus.auction.domain.RuleViolation;
import edu.campus.auction.service.ApiException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrorHandler.class);
    public record ErrorView(String code, String message, Instant timestamp) {}

    private ResponseEntity<ErrorView> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorView(code, message, Instant.now()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorView> api(ApiException e) { return error(e.status, e.code, e.getMessage()); }

    @ExceptionHandler(RuleViolation.class)
    public ResponseEntity<ErrorView> rule(RuleViolation e) {
        return error("LOGIN_REQUIRED".equals(e.code()) ? 401 : 400, e.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorView> validation(MethodArgumentNotValidException e) {
        var field = e.getBindingResult().getFieldErrors().stream().findFirst();
        return error(400, "VALIDATION_ERROR", field.map(f -> f.getField() + ": " + f.getDefaultMessage()).orElse("参数不符合要求"));
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorView> invalidInput(Exception e) {
        return error(400, "VALIDATION_ERROR", "参数格式或范围不符合要求，请检查金额、时间和分页参数");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorView> conflict(DataIntegrityViolationException e) {
        return error(409, "DATA_CONFLICT", "数据重复或冲突，请检查用户名或刷新后重试");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorView> busy(Exception e) {
        return error(409, "BUSY_RETRY", "当前操作较多，请刷新后重试");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorView> notFound(Exception e) { return error(404, "NOT_FOUND", "接口不存在"); }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorView> method(Exception e) { return error(405, "METHOD_NOT_ALLOWED", "请求方法不支持"); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorView> unexpected(Exception e) {
        LOG.error("未处理的接口异常", e);
        return error(500, "INTERNAL_ERROR", "服务暂时异常，请稍后重试");
    }
}
