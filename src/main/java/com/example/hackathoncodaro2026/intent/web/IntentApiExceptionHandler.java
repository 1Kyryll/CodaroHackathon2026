package com.example.hackathoncodaro2026.intent.web;

import com.example.hackathoncodaro2026.exception.ReservationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * JSON error mapping for the {@code /api/**} controllers only. Scoped via
 * {@code assignableTypes} and given the highest precedence so it wins over
 * the app-wide {@code GlobalExceptionHandler} (which returns Thymeleaf
 * redirect view names — meaningless for a JSON client) for exceptions thrown
 * from {@link IntentController} and {@link IntentAuthController}.
 *
 * Never lets an exception escape as a raw stack trace: every branch here
 * returns a clean status code and a short JSON message.
 */
@RestControllerAdvice(assignableTypes = {IntentController.class, IntentAuthController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IntentApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IntentApiExceptionHandler.class);

    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ErrorResponse> handleReservation(ReservationException ex) {
        log.warn("Intent API reservation rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        log.warn("Intent API bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("The request could not be understood."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Authentication required"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception on the intent API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Something went wrong. Please try again."));
    }
}
