
package br.com.fiap.agendafit.dto;

import br.com.fiap.agendafit.model.Role;
import br.com.fiap.agendafit.model.User;

public record UserResponse(Long id, String name, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
