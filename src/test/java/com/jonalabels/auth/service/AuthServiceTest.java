package com.jonalabels.auth.service;

import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.dto.TokenPair;
import com.jonalabels.auth.repository.UsuarioRepository;
import com.jonalabels.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario buildUsuario() {
        return Usuario.builder()
                .id(1L)
                .email("cliente@test.com")
                .passwordHash("$2a$10$hash")
                .rol("CLIENTE")
                .telefono("+525512345678")
                .build();
    }

    @Test
    void login_credencialesValidas_retornaTokenPair() {
        Usuario usuario = buildUsuario();
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarAccessToken(usuario)).thenReturn("access-token-123");
        when(jwtService.generarRefreshToken(usuario)).thenReturn("refresh-token-456");

        TokenPair tokens = authService.login("cliente@test.com", "password");

        assertThat(tokens.accessToken()).isEqualTo("access-token-123");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token-456");
        assertThat(tokens.email()).isEqualTo("cliente@test.com");
        assertThat(tokens.rol()).isEqualTo("CLIENTE");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generarAccessToken(usuario);
        verify(jwtService).generarRefreshToken(usuario);
    }

    @Test
    void login_credencialesInvalidas_lanzaExcepcion() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("invalido@test.com", "wrong"))
                .isInstanceOf(BadCredentialsException.class);

        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    void refresh_tokenValido_retornaNuevoTokenPair() {
        Usuario usuario = buildUsuario();
        when(jwtService.isTokenValid("refresh-valido")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh-valido")).thenReturn(true);
        when(jwtService.extractEmail("refresh-valido")).thenReturn("cliente@test.com");
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarAccessToken(usuario)).thenReturn("nuevo-access");
        when(jwtService.generarRefreshToken(usuario)).thenReturn("nuevo-refresh");

        TokenPair tokens = authService.refresh("refresh-valido");

        assertThat(tokens.accessToken()).isEqualTo("nuevo-access");
        assertThat(tokens.refreshToken()).isEqualTo("nuevo-refresh");
        assertThat(tokens.email()).isEqualTo("cliente@test.com");
    }

    @Test
    void refresh_tokenExpirado_lanzaExcepcion() {
        when(jwtService.isTokenValid("refresh-expirado")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("refresh-expirado"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token inválido o expirado");

        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    void refresh_accessToken_lanzaExcepcion() {
        when(jwtService.isTokenValid("access-token")).thenReturn(true);
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("access-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token inválido o expirado");

        verify(usuarioRepository, never()).findByEmail(anyString());
    }
}
