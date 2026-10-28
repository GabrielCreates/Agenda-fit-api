package br.com.fiap.agendafit.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.function.IntFunction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.fiap.agendafit.assembler.EnrollmentModelAssembler;
import br.com.fiap.agendafit.assembler.GymClassModelAssembler;
import br.com.fiap.agendafit.dto.*;
import br.com.fiap.agendafit.model.*;
import br.com.fiap.agendafit.security.CurrentUser;
import br.com.fiap.agendafit.service.EnrollmentService;
import br.com.fiap.agendafit.service.GymClassService;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "Aulas", description = "Grade de aulas e gerenciamento")
public class GymClassController {

    private final GymClassService service;
    private final EnrollmentService enrollmentService;
    private final GymClassModelAssembler assembler;
    private final EnrollmentModelAssembler enrollmentAssembler;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "Lista aulas paginadas por horário")
    public PagedModel<EntityModel<GymClassResponse>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "startsAt") Pageable pageable,
            PagedResourcesAssembler<GymClass> pagedAssembler) {
        return toPage(service.findAll(pageable), pagedAssembler);
    }

    @GetMapping("/example")
    @Operation(summary = "Busca aulas por Query by Example")
    public PagedModel<EntityModel<GymClassResponse>> findByExample(
            @Parameter(description = "Modalidade: YOGA, PILATES, SPINNING, FUNCTIONAL ou DANCE")
            @RequestParam(required = false) Modality modality,
            @Parameter(description = "Nível: BEGINNER, INTERMEDIATE ou ADVANCED")
            @RequestParam(required = false) Level level,
            @Parameter(description = "Nome do instrutor (ignora maiúsculas e minúsculas)")
            @RequestParam(required = false) String instructor,
            @ParameterObject @PageableDefault(size = 10, sort = "startsAt") Pageable pageable,
            PagedResourcesAssembler<GymClass> pagedAssembler) {
        return toPage(service.findByExample(modality, level, instructor, pageable), pagedAssembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca aulas com Specifications")
    public PagedModel<EntityModel<GymClassResponse>> search(
            @Parameter(description = "Trecho do título (contém, sem diferenciar caixa)")
            @RequestParam(required = false) String title,
            @Parameter(description = "Modalidade: YOGA, PILATES, SPINNING, FUNCTIONAL ou DANCE")
            @RequestParam(required = false) Modality modality,
            @Parameter(description = "Nível: BEGINNER, INTERMEDIATE ou ADVANCED")
            @RequestParam(required = false) Level level,
            @Parameter(description = "Aulas que começam a partir desta data/hora (ISO, ex.: 2026-11-03T07:00:00)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAfter,
            @Parameter(description = "Aulas que começam até esta data/hora (ISO, ex.: 2026-11-03T23:59:00)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsBefore,
            @Parameter(description = "Se true, devolve apenas aulas com vagas disponíveis")
            @RequestParam(required = false) Boolean availableOnly,
            @ParameterObject @PageableDefault(size = 10, sort = "startsAt") Pageable pageable,
            PagedResourcesAssembler<GymClass> pagedAssembler) {
        return toPage(service.search(
                title, modality, level, startsAfter, startsBefore, availableOnly, pageable), pagedAssembler);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma aula pelo ID")
    @ApiResponse(responseCode = "404", description = "Aula não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EntityModel<GymClassResponse> findById(@PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma aula (ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Aula criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<GymClassResponse>> create(
            @RequestBody @Valid GymClassRequest request) {
        var model = assembler.toModel(service.create(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma aula (ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Aula não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EntityModel<GymClassResponse> update(
            @PathVariable Long id, @RequestBody @Valid GymClassRequest request) {
        return assembler.toModel(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma aula (ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Aula excluída")
    @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Aula não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enrollments")
    @Operation(summary = "Inscreve o usuário logado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Inscrição criada")
    @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Aula não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Aula lotada ou usuário já inscrito",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<EnrollmentResponse>> enroll(@PathVariable Long id) {
        String email = currentUser.email().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentAssembler.toModel(enrollmentService.enroll(id, email)));
    }

    private PagedModel<EntityModel<GymClassResponse>> toPage(
            Page<GymClass> page, PagedResourcesAssembler<GymClass> pagedAssembler) {
        var model = pagedAssembler.toModel(page, assembler);
        model.removeLinks();

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        UriComponentsBuilder base = ServletUriComponentsBuilder.fromCurrentRequestUri();

        request.getParameterMap().forEach((name, values) -> {
            if (!name.equals("page") && !name.equals("size") && !name.equals("sort")) {
                for (String value : values) base.queryParam(name, value);
            }
        });

        IntFunction<String> pageUrl = number -> {
            var builder = base.cloneBuilder()
                    .queryParam("page", number)
                    .queryParam("size", page.getSize());

            page.getSort().forEach(order -> builder.queryParam("sort",
                    order.getProperty() + "," + order.getDirection().name().toLowerCase(Locale.ROOT)));

            return builder.build().encode().toUriString();
        };

        model.add(Link.of(pageUrl.apply(0), IanaLinkRelations.FIRST));
        if (page.hasPrevious()) {
            model.add(Link.of(pageUrl.apply(page.getNumber() - 1), IanaLinkRelations.PREV));
        }
        model.add(Link.of(pageUrl.apply(page.getNumber()), IanaLinkRelations.SELF));
        if (page.hasNext()) {
            model.add(Link.of(pageUrl.apply(page.getNumber() + 1), IanaLinkRelations.NEXT));
        }
        if (page.getTotalPages() > 0) {
            model.add(Link.of(pageUrl.apply(page.getTotalPages() - 1), IanaLinkRelations.LAST));
        }
        if (currentUser.isAdmin()) {
            model.add(linkTo(GymClassController.class).withRel("create"));
        }
        return model;
    }
}