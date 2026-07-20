package com.jonalabels.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequestDTO(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        String rol,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(min = 10, max = 15, message = "El teléfono debe tener entre 10 y 15 caracteres")
        @Pattern(regexp = "^\\+?[0-9]+$", message = "El teléfono solo debe contener dígitos y opcionalmente un '+' al inicio")
        String telefono
) {
}
