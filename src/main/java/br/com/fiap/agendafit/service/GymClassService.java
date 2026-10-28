package br.com.fiap.agendafit.service;

import java.time.LocalDateTime;

import br.com.fiap.agendafit.dto.GymClassRequest;
import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.model.Level;
import br.com.fiap.agendafit.model.Modality;
import br.com.fiap.agendafit.repository.EnrollmentRepository;
import br.com.fiap.agendafit.repository.GymClassRepository;
import br.com.fiap.agendafit.repository.GymClassSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GymClassService {

    private final GymClassRepository repository;
    private final EnrollmentRepository enrollmentRepository;

    public Page<GymClass> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Query by Example: o probe leva só os filtros recebidos; o matcher ignora os nulos
     * e compara os textos sem diferenciar maiúsculas de minúsculas.
     */
    public Page<GymClass> findByExample(Modality modality, Level level, String instructor, Pageable pageable) {
        GymClass probe = GymClass.builder()
                .modality(modality)
                .level(level)
                .instructor(StringUtils.hasText(instructor) ? instructor.trim() : null)
                .build();
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT);
        return repository.findAll(Example.of(probe, matcher), pageable);
    }

    /** Specification: cada filtro informado vira uma Specification, todas combinadas com and. */
    public Page<GymClass> search(String title, Modality modality, Level level, LocalDateTime startsAfter,
                                 LocalDateTime startsBefore, Boolean availableOnly, Pageable pageable) {
        Specification<GymClass> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(title)) {
            spec = spec.and(GymClassSpecifications.titleContains(title));
        }
        if (modality != null) {
            spec = spec.and(GymClassSpecifications.modalityEquals(modality));
        }
        if (level != null) {
            spec = spec.and(GymClassSpecifications.levelEquals(level));
        }
        if (startsAfter != null) {
            spec = spec.and(GymClassSpecifications.startsAfter(startsAfter));
        }
        if (startsBefore != null) {
            spec = spec.and(GymClassSpecifications.startsBefore(startsBefore));
        }
        if (Boolean.TRUE.equals(availableOnly)) {
            spec = spec.and(GymClassSpecifications.available());
        }
        return repository.findAll(spec, pageable);
    }

    public GymClass findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
    }

    public GymClass create(GymClassRequest request) {
        return repository.save(request.toEntity());
    }

    public GymClass update(Long id, GymClassRequest request) {
        findById(id);
        GymClass gymClass = request.toEntity();
        gymClass.setId(id);
        return repository.save(gymClass);
    }

    // apaga as inscrições antes, senão a chave estrangeira impede o delete
    @Transactional
    public void delete(Long id) {
        GymClass gymClass = findById(id);
        enrollmentRepository.deleteByGymClassId(id);
        repository.delete(gymClass);
    }
}
