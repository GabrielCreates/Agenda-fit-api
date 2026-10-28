
package br.com.fiap.agendafit.repository;

import br.com.fiap.agendafit.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    void deleteByGymClassId(Long gymClassId);

    long countByGymClassId(Long gymClassId);

    boolean existsByUserIdAndGymClassId(Long userId, Long gymClassId);

    boolean existsByUserEmailAndGymClassId(String email, Long gymClassId);

    Page<Enrollment> findByUserEmail(String email, Pageable pageable);
}
