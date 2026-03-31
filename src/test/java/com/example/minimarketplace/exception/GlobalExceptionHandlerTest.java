package com.example.minimarketplace.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericShouldExposeRootCauseDetails() {
        var model = new ExtendedModelMap();
        var request = new MockHttpServletRequest("GET", "/auth/register");
        var ex = new IllegalStateException("Registration failed", new IllegalArgumentException("ROLE_BUYER missing"));

        String view = handler.handleGeneric(ex, model, request);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("errorCode")).isEqualTo("500");
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Something went wrong");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("ROLE_BUYER missing");
        assertThat(model.getAttribute("errorType")).isEqualTo("IllegalArgumentException");
        assertThat(model.getAttribute("requestPath")).isEqualTo("/auth/register");
    }

    @Test
    void handleRuntimeShouldKeepRequestPathAndMessage() {
        var model = new ExtendedModelMap();
        var request = new MockHttpServletRequest("POST", "/auth/register");
        var ex = new RuntimeException("Username already exists");

        String view = handler.handleRuntime(ex, model, request);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("errorCode")).isEqualTo("400");
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Request failed");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Username already exists");
        assertThat(model.getAttribute("errorType")).isEqualTo("RuntimeException");
        assertThat(model.getAttribute("requestPath")).isEqualTo("/auth/register");
    }
}
