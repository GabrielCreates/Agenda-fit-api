package br.com.fiap.agendafit.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import br.com.fiap.agendafit.controller.EnrollmentController;
import br.com.fiap.agendafit.controller.GymClassController;
import br.com.fiap.agendafit.dto.EnrollmentResponse;
import br.com.fiap.agendafit.model.Enrollment;
import br.com.fiap.agendafit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollmentModelAssembler implements RepresentationModelAssembler<Enrollment, EntityModel<EnrollmentResponse>> {

    private final CurrentUser currentUser;

    @Override
    public EntityModel<EnrollmentResponse> toModel(Enrollment enrollment) {
        EntityModel<EnrollmentResponse> model = EntityModel.of(EnrollmentResponse.from(enrollment));
        model.add(linkTo(GymClassController.class).slash(enrollment.getGymClass().getId()).withRel("class"));

        boolean owner = currentUser.email()
                .map(email -> email.equals(enrollment.getUser().getEmail()))
                .orElse(false);
        if (owner || currentUser.isAdmin()) {
            model.add(linkTo(EnrollmentController.class).slash(enrollment.getId()).withRel("cancel"));
        }
        return model;
    }
}
