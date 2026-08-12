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
import static org.mockito.Mockito.doThrow;
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

    private static final String REGISTRO_TEMPLATE = """
            {
                "email": "%s",
                "password": "%s",
                "rol": "%s",
                "telefono": "%s"
            }
            """;

    @Test
    void registro_datosValidos_retorna201() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "nuevo@test.com", "123456", "CLIENTE", "+525512345678")))
                .andExpect(status().isCreated());
    }

    @Test
    void registro_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "no-es-email", "123456", "CLIENTE", "+525512345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void registro_emailVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "", "123456", "CLIENTE", "+525512345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void registro_passwordCorta_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "test@test.com", "123", "CLIENTE", "+525512345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void registro_rolVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "test@test.com", "123456", "", "+525512345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void registro_telefonoVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "test@test.com", "123456", "CLIENTE", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void registro_telefonoFormatoInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "test@test.com", "123456", "CLIENTE", "abc-def-ghij")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void registro_telefonoMuyCorto_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "test@test.com", "123456", "CLIENTE", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void registro_emailDuplicado_retorna409ConMensajeGenerico() throws Exception {
        org.mockito.Mockito.doThrow(
                        new IllegalStateException("No se pudo completar el registro. Intente nuevamente."))
                .when(authService).registrar(
                        org.mockito.ArgumentMatchers.any(com.jonalabels.auth.dto.RegistroRequestDTO.class));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_TEMPLATE.formatted(
                                "dup@test.com", "123456", "CLIENTE", "+525512345678")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("No se pudo completar el registro. Intente nuevamente."));
    }

    @Test
    void login_credencialesValidas_retorna200ConCookiesHttpOnly() throws Exception {
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
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
                    assertThat(cookies).hasSizeGreaterThanOrEqualTo(2);
                    assertThat(cookies).anyMatch(c ->
                            c.contains("access_token=") && c.contains("HttpOnly") && c.contains("Path=/api"));
                    assertThat(cookies).anyMatch(c ->
                            c.contains("refresh_token=") && c.contains("HttpOnly") && c.contains("Path=/api/v1/auth"));
                });
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        when(authService.login("invalido@test.com", "wrong"))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "invalido@test.com",
                                    "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void login_emailVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "",
                                    "password": "123456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void login_passwordVacia_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@test.com",
                                    "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    void refresh_tokenValido_retorna200ConNuevasCookies() throws Exception {
        when(authService.refresh("refresh-xyz")).thenReturn(SAMPLE_TOKENS);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-xyz")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@test.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
                    assertThat(cookies).anyMatch(c ->
                            c.contains("access_token=") && c.contains("HttpOnly"));
                    assertThat(cookies).anyMatch(c ->
                            c.contains("refresh_token=") && c.contains("HttpOnly"));
                });
    }

    @Test
    void refresh_sinCookie_retorna401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_tokenInvalido_retorna401() throws Exception {
        when(authService.refresh("token-invalido"))
                .thenThrow(new BadCredentialsException("Refresh token inválido o expirado"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "token-invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_retorna200LimpiaCookies() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
                    assertThat(cookies).anyMatch(c ->
                            c.contains("access_token=") && c.contains("Max-Age=0") && c.contains("HttpOnly"));
                    assertThat(cookies).anyMatch(c ->
                            c.contains("refresh_token=") && c.contains("Max-Age=0") && c.contains("HttpOnly"));
                });
    }
}
