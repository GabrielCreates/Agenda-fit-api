
package br.com.fiap.agendafit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must have at most 100 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        @Size(max = 150, message = "email must have at most 150 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 72, message = "password must have between 6 and 72 characters")
        String password
) {}
