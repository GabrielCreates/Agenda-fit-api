
package br.com.fiap.agendafit.dto;

import java.time.LocalDateTime;

import br.com.fiap.agendafit.model.Enrollment;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "enrollments", itemRelation = "enrollment")
public record EnrollmentResponse(
        Long id,
        Long classId,
        String classTitle,
        LocalDateTime startsAt,
        LocalDateTime createdAt
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        var gymClass = enrollment.getGymClass();

        return new EnrollmentResponse(
                enrollment.getId(),
                gymClass.getId(),
                gymClass.getTitle(),
                gymClass.getStartsAt(),
                enrollment.getCreatedAt()
        );
    }
}
