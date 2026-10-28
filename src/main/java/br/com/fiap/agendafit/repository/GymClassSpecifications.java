
package br.com.fiap.agendafit.repository;

import java.time.LocalDateTime;

import br.com.fiap.agendafit.model.Enrollment;
import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.model.Level;
import br.com.fiap.agendafit.model.Modality;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class GymClassSpecifications {

    private GymClassSpecifications() {}

    public static Specification<GymClass> titleContains(String title) {
        return (root, query, cb) -> {
            String escaped = title.trim().toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");

            return cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\');
        };
    }

    public static Specification<GymClass> modalityEquals(Modality modality) {
        return (root, query, cb) -> cb.equal(root.get("modality"), modality);
    }

    public static Specification<GymClass> levelEquals(Level level) {
        return (root, query, cb) -> cb.equal(root.get("level"), level);
    }

    public static Specification<GymClass> startsAfter(LocalDateTime from) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.<LocalDateTime>get("startsAt"), from);
    }

    public static Specification<GymClass> startsBefore(LocalDateTime to) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.<LocalDateTime>get("startsAt"), to);
    }

    public static Specification<GymClass> available() {
        return (root, query, cb) -> {
            Subquery<Long> enrolled = query.subquery(Long.class);
            Root<Enrollment> enrollment = enrolled.from(Enrollment.class);

            enrolled.select(cb.count(enrollment))
                    .where(cb.equal(
                            enrollment.get("gymClass").get("id"),
                            root.get("id")
                    ));

            return cb.gt(root.<Number>get("capacity"), enrolled);
        };
    }
}
