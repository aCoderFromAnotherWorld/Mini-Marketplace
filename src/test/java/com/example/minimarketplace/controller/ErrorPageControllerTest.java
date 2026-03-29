package com.example.minimarketplace.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorPageControllerTest {

    private final ErrorPageController controller = new ErrorPageController();

    @Test
    void accessDeniedPageShouldExposeExactSecurityFailureDetails() {
        var request = new MockHttpServletRequest("POST", "/access-denied");
        var model = new ExtendedModelMap();

        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/auth/register");
        request.setAttribute("errorTitle", "Invalid form token");
        request.setAttribute("errorMessage", "Your session token is missing or expired. Refresh the page and submit the form again.");
        request.setAttribute("errorType", "MissingCsrfTokenException");

        String view = controller.accessDeniedPage(request, model);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("errorCode")).isEqualTo("403");
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Invalid form token");
        assertThat(model.getAttribute("errorMessage"))
            .isEqualTo("Your session token is missing or expired. Refresh the page and submit the form again.");
        assertThat(model.getAttribute("errorType")).isEqualTo("MissingCsrfTokenException");
        assertThat(model.getAttribute("requestPath")).isEqualTo("/auth/register");
    }
}
