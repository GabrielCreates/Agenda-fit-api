package br.com.fiap.agendafit;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.agendafit.model.Enrollment;
import br.com.fiap.agendafit.model.GymClass;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class EnrollmentTest extends AbstractApiTest {
    private static final String AUTH = HttpHeaders.AUTHORIZATION;

    @Test
    void inscricaoValidaDevolve201ComOContrato() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments").header(AUTH, ana()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.classId").value(aula.getId()))
                .andExpect(jsonPath("$.classTitle").value("Aula de teste"))
                .andExpect(jsonPath("$.startsAt").value("2030-01-01T10:00:00"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$._links.class.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists());
        Assertions.assertThat(enrollments.existsByUserEmailAndGymClassId("ana@fit.com", aula.getId())).isTrue();
    }

    @Test
    void inscricaoDuplicadaDevolve409() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments").header(AUTH, ana()))
                .andExpect(status().isCreated());
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments").header(AUTH, ana()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isString());
        Assertions.assertThat(enrollments.countByGymClassId(aula.getId())).isEqualTo(1);
    }

    @Test
    void aulaLotadaDevolve409() throws Exception {
        GymClass aula = newClass(1);
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments").header(AUTH, ana()))
                .andExpect(status().isCreated());
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments").header(AUTH, bruno()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isString());
        Assertions.assertThat(enrollments.countByGymClassId(aula.getId())).isEqualTo(1);
    }

    @Test
    void aulaLotadaDoDataLoaderDevolve409() throws Exception {
        GymClass lotada = classes.findAll().stream()
                .filter(c -> enrollments.countByGymClassId(c.getId()) >= c.getCapacity())
                .findFirst().orElseThrow();
        mvc.perform(post("/classes/" + lotada.getId() + "/enrollments").header(AUTH, admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void aulaInexistenteDevolve404() throws Exception {
        mvc.perform(post("/classes/999999/enrollments").header(AUTH, ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Class not found"));
    }

    @Test
    void identidadeVemDoTokenEIdsNoCorpoSaoIgnorados() throws Exception {
        GymClass aula = newClass(5);
        Long idDoBruno = users.findByEmail("bruno@fit.com").orElseThrow().getId();
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments")
                        .header(AUTH, ana()).contentType("application/json")
                        .content("{\"userId\":" + idDoBruno + "}"))
                .andExpect(status().isCreated());
        Assertions.assertThat(enrollments.existsByUserEmailAndGymClassId("ana@fit.com", aula.getId())).isTrue();
        Assertions.assertThat(enrollments.existsByUserEmailAndGymClassId("bruno@fit.com", aula.getId())).isFalse();
    }

    @Test
    void listaSoAsProprias() throws Exception {
        GymClass aula = newClass(5);
        enroll("ana@fit.com", aula);
        enroll("bruno@fit.com", aula);
        long daAna = enrollments.findAll().stream()
                .filter(e -> e.getUser().getEmail().equals("ana@fit.com")).count();
        mvc.perform(get("/enrollments/me").param("size", "100").header(AUTH, ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(daAna))
                .andExpect(jsonPath("$._embedded.enrollments[?(@.classId == " + aula.getId() + ")]", hasSize(1)));
        mvc.perform(get("/enrollments/me").param("size", "100").header(AUTH, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    void listaEhPaginada() throws Exception {
        long daAna = enrollments.findAll().stream()
                .filter(e -> e.getUser().getEmail().equals("ana@fit.com")).count();
        Assertions.assertThat(daAna).isGreaterThan(2);
        mvc.perform(get("/enrollments/me").param("size", "2").param("page", "0").header(AUTH, ana()))
                .andExpect(jsonPath("$._embedded.enrollments", hasSize(2)))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(daAna))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$._links.next.href").exists());
    }

    @Test
    void donoCancelaAPropriaInscricao() throws Exception {
        Enrollment e = enroll("ana@fit.com", newClass(5));
        mvc.perform(delete("/enrollments/" + e.getId()).header(AUTH, ana()))
                .andExpect(status().isNoContent());
        Assertions.assertThat(enrollments.existsById(e.getId())).isFalse();
    }

    @Test
    void outroUsuarioNaoCancelaInscricaoAlheia() throws Exception {
        Enrollment e = enroll("ana@fit.com", newClass(5));
        mvc.perform(delete("/enrollments/" + e.getId()).header(AUTH, bruno()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isString());
        Assertions.assertThat(enrollments.existsById(e.getId())).isTrue();
    }

    @Test
    void adminCancelaQualquerInscricao() throws Exception {
        Enrollment e = enroll("ana@fit.com", newClass(5));
        mvc.perform(delete("/enrollments/" + e.getId()).header(AUTH, admin()))
                .andExpect(status().isNoContent());
        Assertions.assertThat(enrollments.existsById(e.getId())).isFalse();
    }

    @Test
    void cancelarInscricaoInexistenteDevolve404() throws Exception {
        mvc.perform(delete("/enrollments/999999").header(AUTH, ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void cancelarLiberaAVaga() throws Exception {
        GymClass aula = newClass(1);
        Enrollment e = enroll("ana@fit.com", aula);
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments").header(AUTH, bruno()))
                .andExpect(status().isConflict());
        mvc.perform(delete("/enrollments/" + e.getId()).header(AUTH, ana()))
                .andExpect(status().isNoContent());
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments").header(AUTH, bruno()))
                .andExpect(status().isCreated());
    }
}