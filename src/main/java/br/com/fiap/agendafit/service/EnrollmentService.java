package br.com.fiap.agendafit.service;

import java.time.LocalDateTime;

import br.com.fiap.agendafit.model.Enrollment;
import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.model.User;
import br.com.fiap.agendafit.repository.EnrollmentRepository;
import br.com.fiap.agendafit.repository.GymClassRepository;
import br.com.fiap.agendafit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final GymClassRepository gymClassRepository;
    private final UserRepository userRepository;

    @Transactional
    public Enrollment enroll(Long classId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        GymClass gymClass = gymClassRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        if (enrollmentRepository.existsByUserIdAndGymClassId(user.getId(), classId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this class");
        }
        if (enrollmentRepository.countByGymClassId(classId) >= gymClass.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class is full");
        }
        try {
            return enrollmentRepository.saveAndFlush(Enrollment.builder()
                    .user(user)
                    .gymClass(gymClass)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this class");
        }
    }

    public Page<Enrollment> findMine(String email, Pageable pageable) {
        return enrollmentRepository.findByUserEmail(email, pageable);
    }

    @Transactional
    public void cancel(Long id, String email, boolean admin) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        if (!admin && !enrollment.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own enrollments");
        }
        enrollmentRepository.delete(enrollment);
    }
}
