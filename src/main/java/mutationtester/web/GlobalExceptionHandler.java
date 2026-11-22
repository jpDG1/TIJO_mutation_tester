package mutationtester.web;

import mutationtester.service.AnalysisRejectedException;
import mutationtester.web.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice(basePackageClasses = MutationRunController.class)
public final class GlobalExceptionHandler {

    @ExceptionHandler(AnalysisRejectedException.class)
    public ResponseEntity<ApiErrorResponse> handleRejectedAnalysis(AnalysisRejectedException exception) {
        return ResponseEntity.unprocessableEntity()
                .body(new ApiErrorResponse(exception.getMessage(), exception.diagnostics()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingUpload(MissingServletRequestPartException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("Missing required file: " + exception.getRequestPartName(), List.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("Invalid value for " + exception.getName() + '.', List.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiErrorResponse(exception.getReason(), List.of()));
    }
}
