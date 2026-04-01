package com.example.minimarketplace.controller;

import com.example.minimarketplace.entity.SellerRequest;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void dashboardShouldPopulatePendingRequestsAndUsers() {
        var model = new ExtendedModelMap();
        var pendingRequest = SellerRequest.builder().id(3L).build();
        var user = User.builder().username("buyer1").build();

        when(userService.getPendingRequests()).thenReturn(List.of(pendingRequest));
        when(userService.findAll()).thenReturn(List.of(user));

        String view = adminController.dashboard(model);

        assertThat(view).isEqualTo("admin/dashboard");
        assertThat(model.getAttribute("pendingRequests")).isEqualTo(List.of(pendingRequest));
        assertThat(model.getAttribute("allUsers")).isEqualTo(List.of(user));
    }

    @Test
    void approveShouldAddSuccessFlashMessage() {
        var redirectAttributes = new RedirectAttributesModelMap();

        String view = adminController.approve(9L, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/dashboard");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg"))
            .isEqualTo("Request approved.");
        verify(userService).approveSellerRequest(9L);
    }

    @Test
    void rejectShouldAddErrorFlashMessageWhenServiceFails() {
        var redirectAttributes = new RedirectAttributesModelMap();

        doThrow(new RuntimeException("Request not found: 9"))
            .when(userService).rejectSellerRequest(9L);

        String view = adminController.reject(9L, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/dashboard");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMsg"))
            .isEqualTo("Request not found: 9");
    }
}
