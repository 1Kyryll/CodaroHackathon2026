package com.example.hackathoncodaro2026;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoggingWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void echoesSafeRequestIdAndClearsMdc() throws Exception {
        mockMvc.perform(get("/login").header("X-Request-ID", "abc12345-safe-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "abc12345-safe-id"));
        assertThat(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty()).isTrue();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void rejectsUnsafeRequestIdAndIssuesNewOne() throws Exception {
        MvcResult result = mockMvc.perform(get("/login").header("X-Request-ID", "bad id\ninject"))
                .andExpect(status().isOk())
                .andReturn();
        String issued = result.getResponse().getHeader("X-Request-ID");
        assertThat(issued).isNotBlank();
        assertThat(issued).isNotEqualTo("bad id\ninject");
        assertThat(issued).doesNotContain(" ");
        assertThat(issued).doesNotContain("\n");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void loginSuccessRedirectsHome() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("Admin123!"))
                .andExpect(authenticated().withUsername("admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginFailureRedirectsWithError() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void logoutRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
