package com.jonalabels.auth.config;

import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.admin.seed-enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.seed-password:}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            log.warn("Admin seed skipped: set APP_ADMIN_SEED_PASSWORD to create the default admin user");
            return;
        }

        if (usuarioRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists — skipping seed");
            return;
        }

        Usuario admin = Usuario.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .rol("ADMIN")
                .build();

        usuarioRepository.save(admin);
        log.info("Admin user created: {}", adminEmail);
    }
}
