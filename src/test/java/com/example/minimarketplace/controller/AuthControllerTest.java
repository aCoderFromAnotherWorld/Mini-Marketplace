package com.example.minimarketplace.controller;

import com.example.minimarketplace.config.SecurityConfig;
import com.example.minimarketplace.dto.RegisterRequest;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginPageShouldRedirectAuthenticatedUsers() {
        var auth = new TestingAuthenticationToken("buyer1", "pw", "ROLE_BUYER");
        var session = new MockHttpSession();
        var model = new ExtendedModelMap();

        String view = authController.loginPage(null, null, null, auth, session, model);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    void loginPageShouldConsumeStoredAuthErrorMessage() {
        var session = new MockHttpSession();
        var model = new ExtendedModelMap();
        session.setAttribute(SecurityConfig.AUTH_ERROR_MESSAGE_SESSION_KEY, "Your session expired. Please try again.");

        String view = authController.loginPage(null, null, null, null, session, model);

        assertThat(view).isEqualTo("auth/login");
        assertThat(model.getAttribute("errorMsg")).isEqualTo("Your session expired. Please try again.");
        assertThat(session.getAttribute(SecurityConfig.AUTH_ERROR_MESSAGE_SESSION_KEY)).isNull();
    }

    @Test
    void registerPageShouldPrepareFormForGuests() {
        var session = new MockHttpSession();
        var model = new ExtendedModelMap();

        String view = authController.registerPage(null, null, session, model);

        assertThat(view).isEqualTo("auth/register");
        assertThat(model.getAttribute("registerRequest")).isInstanceOf(RegisterRequest.class);
    }

    @Test
    void registerShouldReturnFormViewWhenValidationFails() {
        var request = new RegisterRequest("ab", "invalid-email", "123");
        var bindingResult = new BeanPropertyBindingResult(request, "registerRequest");
        var redirectAttributes = new RedirectAttributesModelMap();
        var model = new ExtendedModelMap();
        bindingResult.rejectValue("username", "invalid", "Username is invalid");

        String view = authController.register(request, bindingResult, redirectAttributes, model);

        assertThat(view).isEqualTo("auth/register");
        verifyNoInteractions(userService);
    }

    @Test
    void registerShouldRedirectToLoginAfterSuccessfulRegistration() {
        var request = new RegisterRequest("buyer1", "buyer1@example.com", "secret123");
        var bindingResult = new BeanPropertyBindingResult(request, "registerRequest");
        var redirectAttributes = new RedirectAttributesModelMap();
        var model = new ExtendedModelMap();

        when(userService.register(request)).thenReturn(User.builder().username("buyer1").build());

        String view = authController.register(request, bindingResult, redirectAttributes, model);

        assertThat(view).isEqualTo("redirect:/auth/login");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg"))
            .isEqualTo("Account created! You can now log in.");
    }

    @Test
    void registerShouldShowErrorWhenServiceThrows() {
        var request = new RegisterRequest("buyer1", "buyer1@example.com", "secret123");
        var bindingResult = new BeanPropertyBindingResult(request, "registerRequest");
        var redirectAttributes = new RedirectAttributesModelMap();
        var model = new ExtendedModelMap();

        when(userService.register(request)).thenThrow(new RuntimeException("Username 'buyer1' is already taken."));

        String view = authController.register(request, bindingResult, redirectAttributes, model);

        assertThat(view).isEqualTo("auth/register");
        assertThat(model.getAttribute("errorMsg")).isEqualTo("Username 'buyer1' is already taken.");
    }
}
