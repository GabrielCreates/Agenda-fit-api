
package br.com.fiap.agendafit.repository;

import java.util.Optional;

import br.com.fiap.agendafit.model.GymClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GymClassRepository
        extends JpaRepository<GymClass, Long>, JpaSpecificationExecutor<GymClass> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GymClass g where g.id = :id")
    Optional<GymClass> findByIdForUpdate(@Param("id") Long id);
}
