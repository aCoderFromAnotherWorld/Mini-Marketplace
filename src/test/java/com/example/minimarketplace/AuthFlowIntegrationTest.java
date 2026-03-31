package com.example.minimarketplace;

import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.docker.compose.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void loginPageShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/auth/login"))
            .andExpect(status().isOk());
    }

    @Test
    void loginPageWithTrailingSlashShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/auth/login/"))
            .andExpect(status().isOk());
    }

    @Test
    void loginPageWithJsessionIdPathParamShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/auth/login;jsessionid=fake-session"))
            .andExpect(status().isOk());
    }

    @Test
    void registerPageWithTrailingSlashShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/auth/register/"))
            .andExpect(status().isOk());
    }

    @Test
    void registerShouldCreateUserAndRedirectToLogin() throws Exception {
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .param("username", "buyer_test")
                .param("email", "buyer_test@example.com")
                .param("password", "secret123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login"));

        var created = userRepository.findByUsername("buyer_test");
        assertThat(created).isPresent();
        assertThat(created.get().isBuyer()).isTrue();
    }

    @Test
    void registerWithoutCsrfShouldShowExactForbiddenReason() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                .accept(MediaType.TEXT_HTML)
                .param("username", "buyer_test_2")
                .param("email", "buyer_test_2@example.com")
                .param("password", "secret123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/register"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/auth/register").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Your session expired. Please submit the form again.")));
    }

    @Test
    void loginWithSeedAdminShouldRedirectHome() throws Exception {
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "admin")
                .param("password", "admin123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }
}
