
package br.com.fiap.agendafit.security;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private Optional<Authentication> authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        return Optional.of(auth);
    }

    public Optional<String> email() {
        return authentication().map(Authentication::getName);
    }

    public boolean isAdmin() {
        return authentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(authority ->
                                "ROLE_ADMIN".equals(authority.getAuthority())))
                .orElse(false);
    }
}
