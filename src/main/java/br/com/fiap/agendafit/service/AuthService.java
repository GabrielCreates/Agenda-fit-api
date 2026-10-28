package br.com.fiap.agendafit.service;

import br.com.fiap.agendafit.dto.LoginRequest;
import br.com.fiap.agendafit.dto.LoginResponse;
import br.com.fiap.agendafit.dto.RegisterRequest;
import br.com.fiap.agendafit.model.Role;
import br.com.fiap.agendafit.model.User;
import br.com.fiap.agendafit.repository.UserRepository;
import br.com.fiap.agendafit.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public User register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw emailInUse();
        }
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw emailInUse();
        }
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return tokenService.generate(user);
    }

    private ResponseStatusException emailInUse() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
