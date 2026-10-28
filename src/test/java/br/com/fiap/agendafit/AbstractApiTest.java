
package br.com.fiap.agendafit;

import java.time.LocalDateTime;

import br.com.fiap.agendafit.model.Enrollment;
import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.model.Level;
import br.com.fiap.agendafit.model.Modality;
import br.com.fiap.agendafit.model.User;
import br.com.fiap.agendafit.repository.EnrollmentRepository;
import br.com.fiap.agendafit.repository.GymClassRepository;
import br.com.fiap.agendafit.repository.UserRepository;
import br.com.fiap.agendafit.security.TokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected GymClassRepository classes;

    @Autowired
    protected EnrollmentRepository enrollments;

    @Autowired
    protected TokenService tokenService;

    protected String bearer(String email) {
        User user = users.findByEmail(email).orElseThrow();
        return "Bearer " + tokenService.generate(user).token();
    }

    protected String admin() {
        return bearer("admin@fit.com");
    }

    protected String ana() {
        return bearer("ana@fit.com");
    }

    protected String bruno() {
        return bearer("bruno@fit.com");
    }

    protected GymClass newClass(int capacity) {
        return newClass(capacity, LocalDateTime.of(2030, 1, 1, 10, 0));
    }

    protected GymClass newClass(int capacity, LocalDateTime startsAt) {
        return classes.save(GymClass.builder()
                .title("Aula de teste")
                .modality(Modality.YOGA)
                .level(Level.BEGINNER)
                .instructor("Instrutor Teste")
                .startsAt(startsAt)
                .durationMinutes(60)
                .capacity(capacity)
                .build());
    }

    protected Enrollment enroll(String email, GymClass gymClass) {
        return enrollments.save(Enrollment.builder()
                .user(users.findByEmail(email).orElseThrow())
                .gymClass(gymClass)
                .createdAt(LocalDateTime.now())
                .build());
    }

    protected static final String CLASS_JSON = """
            {"title":"Funcional ao ar livre","modality":"FUNCTIONAL","level":"INTERMEDIATE",
             "instructor":"Rafael Souza","startsAt":"2030-02-01T10:00:00","durationMinutes":45,"capacity":15}""";
}
