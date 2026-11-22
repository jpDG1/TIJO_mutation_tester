package mutationtester.web.dto;

import java.util.List;

public record ApiErrorResponse(String message, List<String> diagnostics) {
}
