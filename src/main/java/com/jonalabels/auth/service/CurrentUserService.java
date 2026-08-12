package com.jonalabels.auth.service;

import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.repository.UsuarioRepository;
import com.jonalabels.common.exception.RecursoNoEncontradoException;
import com.jonalabels.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    public Usuario requireCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario con email " + email + " no encontrado"));
    }
}
