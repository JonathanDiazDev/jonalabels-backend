package com.jonalabels.auth.service;

import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.dto.TokenPair;
import com.jonalabels.auth.repository.UsuarioRepository;
import com.jonalabels.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public TokenPair login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        return buildTokenPair(usuario);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new org.springframework.security.authentication
                    .BadCredentialsException("Refresh token inválido o expirado");
        }

        String email = jwtService.extractEmail(refreshToken);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.authentication
                        .BadCredentialsException("Usuario no encontrado"));

        return buildTokenPair(usuario);
    }

    private TokenPair buildTokenPair(Usuario usuario) {
        return new TokenPair(
                jwtService.generarAccessToken(usuario),
                jwtService.generarRefreshToken(usuario),
                usuario.getEmail(),
                usuario.getRol());
    }
}
