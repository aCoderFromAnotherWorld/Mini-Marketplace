package com.example.minimarketplace.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletRequest request) {
        populateErrorModel(
            model,
            HttpStatus.FORBIDDEN,
            "Access denied",
            "You don't have permission to access this page.",
            ex,
            request
        );
        return errorView();
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleRuntime(RuntimeException ex, Model model, HttpServletRequest request) {
        log.warn("Runtime exception: {}", ex.getMessage());
        populateErrorModel(model, HttpStatus.BAD_REQUEST, "Request failed", ex.getMessage(), ex, request);
        return errorView();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoResourceFoundException ex, Model model, HttpServletRequest request) {
        populateErrorModel(
            model,
            HttpStatus.NOT_FOUND,
            "Page not found",
            "The page you requested was not found.",
            ex,
            request
        );
        return errorView();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        populateErrorModel(
            model,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong",
            resolveMessage(ex),
            ex,
            request
        );
        return errorView();
    }

    private void populateErrorModel(Model model,
                                    HttpStatus status,
                                    String title,
                                    String message,
                                    Throwable ex,
                                    HttpServletRequest request) {
        Throwable rootCause = getRootCause(ex);

        model.addAttribute("errorCode", Integer.toString(status.value()));
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", StringUtils.hasText(message) ? message : "No error details were provided.");
        model.addAttribute("errorType", rootCause.getClass().getSimpleName());
        model.addAttribute("requestPath", request.getRequestURI());
    }

    private String errorView() {
        return "error";
    }

    private String resolveMessage(Throwable ex) {
        Throwable rootCause = getRootCause(ex);
        if (StringUtils.hasText(rootCause.getMessage())) {
            return rootCause.getMessage();
        }
        if (StringUtils.hasText(ex.getMessage())) {
            return ex.getMessage();
        }
        return "No error details were provided.";
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
