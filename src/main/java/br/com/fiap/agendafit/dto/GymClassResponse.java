
package br.com.fiap.agendafit.dto;

import java.time.LocalDateTime;

import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.model.Level;
import br.com.fiap.agendafit.model.Modality;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "classes", itemRelation = "class")
public record GymClassResponse(
        Long id,
        String title,
        Modality modality,
        Level level,
        String instructor,
        LocalDateTime startsAt,
        Integer durationMinutes,
        Integer capacity,
        int availableSpots
) {
    public static GymClassResponse from(GymClass gymClass, int availableSpots) {
        return new GymClassResponse(
                gymClass.getId(),
                gymClass.getTitle(),
                gymClass.getModality(),
                gymClass.getLevel(),
                gymClass.getInstructor(),
                gymClass.getStartsAt(),
                gymClass.getDurationMinutes(),
                gymClass.getCapacity(),
                availableSpots
        );
    }
}
