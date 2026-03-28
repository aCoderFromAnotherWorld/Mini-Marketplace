package com.example.minimarketplace.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorPageController {

    @RequestMapping("/access-denied")
    public String accessDeniedPage(HttpServletRequest request, Model model) {
        int statusCode = resolveStatusCode(request);
        HttpStatus status = HttpStatus.resolve(statusCode);
        Throwable exception = resolveException(request);

        model.addAttribute("errorCode", Integer.toString(statusCode));
        model.addAttribute("errorTitle", firstNonBlank(
            attributeAsString(request, "errorTitle"),
            defaultTitle(status)
        ));
        model.addAttribute("errorMessage", firstNonBlank(
            attributeAsString(request, "errorMessage"),
            attributeAsString(request, RequestDispatcher.ERROR_MESSAGE),
            exception != null ? exception.getMessage() : null,
            defaultMessage(status)
        ));
        model.addAttribute("errorType", firstNonBlank(
            attributeAsString(request, "errorType"),
            exception != null ? exception.getClass().getSimpleName() : null,
            "UnknownException"
        ));
        model.addAttribute("requestPath", firstNonBlank(
            attributeAsString(request, RequestDispatcher.ERROR_REQUEST_URI),
            request.getRequestURI(),
            "Unknown"
        ));

        return "error";
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (value instanceof Integer statusCode) {
            return statusCode;
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Integer.parseInt(text);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private Throwable resolveException(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        return value instanceof Throwable throwable ? throwable : null;
    }

    private String attributeAsString(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value != null ? value.toString() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String defaultTitle(HttpStatus status) {
        if (status == null) {
            return "Something went wrong";
        }
        return switch (status) {
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Page not found";
            case BAD_REQUEST -> "Request failed";
            case INTERNAL_SERVER_ERROR -> "Something went wrong";
            default -> status.getReasonPhrase();
        };
    }

    private String defaultMessage(HttpStatus status) {
        if (status == null) {
            return "An unexpected error occurred.";
        }
        return switch (status) {
            case FORBIDDEN -> "You don't have permission to access this page.";
            case NOT_FOUND -> "The page you requested was not found.";
            case BAD_REQUEST -> "The request could not be completed.";
            case INTERNAL_SERVER_ERROR -> "An unexpected error occurred.";
            default -> status.getReasonPhrase();
        };
    }
}
