package com.jonalabels.auth.controller;

import com.jonalabels.auth.dto.AuthResponseDTO;
import com.jonalabels.auth.dto.LoginRequestDTO;
import com.jonalabels.auth.dto.RegistroRequestDTO;
import com.jonalabels.auth.dto.TokenPair;
import com.jonalabels.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    private static final long ACCESS_MAX_AGE = 900;       // 15 minutos
    private static final long REFRESH_MAX_AGE = 604800;   // 7 días

    @PostMapping("/registro")
    public ResponseEntity<Void> registro(@Valid @RequestBody RegistroRequestDTO request) {
        authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /*
     * Ambos tokens se devuelven como cookies HttpOnly.
     *
     * ¿Por qué NO el Access Token en el body?
     * Porque obliga al frontend a almacenarlo en JavaScript (state, localStorage, etc.).
     * Cualquier vulnerabilidad XSS podría exfiltrar ese valor.
     *
     * Con HttpOnly, el navegador adjunta automáticamente las cookies en cada request
     * y JavaScript NUNCA puede leerlas, eliminando por completo el vector de robo
     * de tokens vía XSS.
     *
     * En dev, SameSite=Lax permite cookies same-site. En producción, SameSite=None
     * (requiere Secure=true) habilita cookies cross-origin cuando frontend y backend
     * están en dominios distintos (ej: Vercel + Koyeb).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        TokenPair tokens = authService.login(request.email(), request.password());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        buildAccessCookie(tokens.accessToken()).toString(),
                        buildRefreshCookie(tokens.refreshToken()).toString())
                .body(new AuthResponseDTO(tokens.email(), tokens.rol()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenPair tokens = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        buildAccessCookie(tokens.accessToken()).toString(),
                        buildRefreshCookie(tokens.refreshToken()).toString())
                .body(new AuthResponseDTO(tokens.email(), tokens.rol()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api")
                .maxAge(0)
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccess.toString(), clearRefresh.toString())
                .build();
    }

    private ResponseCookie buildAccessCookie(String token) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api")
                .maxAge(ACCESS_MAX_AGE)
                .build();
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(REFRESH_MAX_AGE)
                .build();
    }
}
