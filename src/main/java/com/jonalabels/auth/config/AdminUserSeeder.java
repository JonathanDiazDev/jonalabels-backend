package com.jonalabels.auth.config;

import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = "admin@jonalabels.com";

        if (usuarioRepository.findByEmail(email).isPresent()) {
            log.info("Admin user already exists — skipping seed");
            return;
        }

        Usuario admin = Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .rol("ADMIN")
                .build();

        usuarioRepository.save(admin);
        log.info("Admin user created: {}", email);
    }
}
