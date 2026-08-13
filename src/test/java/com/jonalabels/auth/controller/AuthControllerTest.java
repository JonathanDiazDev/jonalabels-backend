package com.jonalabels.auth.controller;

import com.jonalabels.auth.dto.TokenPair;
import com.jonalabels.auth.service.AuthService;
import com.jonalabels.common.exception.GlobalExceptionHandler;
import com.jonalabels.security.config.RateLimitFilter;
import com.jonalabels.security.config.SecurityConfig;
import com.jonalabels.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RateLimitFilter.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private static final TokenPair SAMPLE_TOKENS =
            new TokenPair("access-abc", "refresh-xyz", "cliente@test.com", "CLIENTE");

    private static final String REGISTRO_BODY = """
            {
                "email": "nuevo@test.com",
                "password": "123456",
                "rol": "ADMIN",
                "telefono": "+525512345678"
            }
            """;

    @Test
    void registro_deshabilitado_retorna403() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_datosValidos_retorna200ConCookies() throws Exception {
        when(authService.login("cliente@test.com", "123456")).thenReturn(SAMPLE_TOKENS);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "cliente@test.com",
                                    "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@test.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).login("cliente@test.com", "123456");
    }

    @Test
    void login_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "no-es-email",
                                    "password": "123456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        when(authService.login("cliente@test.com", "wrong"))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "cliente@test.com",
                                    "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_conCookieValida_retorna200() throws Exception {
        when(authService.refresh("refresh-xyz")).thenReturn(SAMPLE_TOKENS);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-xyz")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@test.com"))
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).refresh("refresh-xyz");
    }

    @Test
    void refresh_sinCookie_retorna401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_retorna200YLimpiaCookies() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookieHeaders).isNotEmpty();
        assertThat(setCookieHeaders.stream().anyMatch(h -> h.contains("access_token="))).isTrue();
        assertThat(setCookieHeaders.stream().anyMatch(h -> h.contains("refresh_token="))).isTrue();
        assertThat(setCookieHeaders.stream().anyMatch(h -> h.contains("Max-Age=0"))).isTrue();
    }
}
