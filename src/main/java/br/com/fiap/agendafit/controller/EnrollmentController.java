package br.com.fiap.agendafit.controller;

import br.com.fiap.agendafit.assembler.EnrollmentModelAssembler;
import br.com.fiap.agendafit.dto.EnrollmentResponse;
import br.com.fiap.agendafit.dto.ErrorResponse;
import br.com.fiap.agendafit.model.Enrollment;
import br.com.fiap.agendafit.security.CurrentUser;
import br.com.fiap.agendafit.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Tag(name = "Inscrições", description = "Inscrições do usuário logado (exige token)")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService service;
    private final EnrollmentModelAssembler assembler;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    @Operation(summary = "Lista, paginadas, as inscrições do usuário logado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PagedModel<EntityModel<EnrollmentResponse>> findMine(
            @ParameterObject @PageableDefault(size = 10, sort = "gymClass.startsAt") Pageable pageable,
            PagedResourcesAssembler<Enrollment> pagedAssembler) {
        String email = currentEmail();
        return pagedAssembler.toModel(service.findMine(email, pageable), assembler);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela uma inscrição (dono ou ADMIN)")
    @ApiResponse(responseCode = "204", description = "Inscrição cancelada")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "A inscrição pertence a outro usuário",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Inscrição não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancel(id, currentEmail(), currentUser.isAdmin());
        return ResponseEntity.noContent().build();
    }

    private String currentEmail() {
        return currentUser.email()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
