package br.com.fiap.agendafit.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import br.com.fiap.agendafit.controller.GymClassController;
import br.com.fiap.agendafit.dto.GymClassResponse;
import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.repository.EnrollmentRepository;
import br.com.fiap.agendafit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GymClassModelAssembler implements RepresentationModelAssembler<GymClass, EntityModel<GymClassResponse>> {

    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUser currentUser;

    @Override
    public EntityModel<GymClassResponse> toModel(GymClass gymClass) {
        Long id = gymClass.getId();
        long enrolled = enrollmentRepository.countByGymClassId(id);
        int availableSpots = (int) Math.max(0, gymClass.getCapacity() - enrolled);

        EntityModel<GymClassResponse> model = EntityModel.of(GymClassResponse.from(gymClass, availableSpots));
        model.add(linkTo(GymClassController.class).slash(id).withSelfRel());
        model.add(linkTo(GymClassController.class).withRel("classes"));

        currentUser.email().ifPresent(email -> {
            if (availableSpots > 0 && !enrollmentRepository.existsByUserEmailAndGymClassId(email, id)) {
                model.add(linkTo(GymClassController.class).slash(id).slash("enrollments").withRel("enroll"));
            }
        });

        if (currentUser.isAdmin()) {
            model.add(linkTo(GymClassController.class).slash(id).withRel("update"));
            model.add(linkTo(GymClassController.class).slash(id).withRel("delete"));
        }
        return model;
    }
}
