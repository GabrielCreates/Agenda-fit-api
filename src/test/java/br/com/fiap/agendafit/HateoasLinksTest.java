package br.com.fiap.agendafit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.agendafit.assembler.EnrollmentModelAssembler;
import br.com.fiap.agendafit.dto.EnrollmentResponse;
import br.com.fiap.agendafit.model.Enrollment;
import br.com.fiap.agendafit.model.GymClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HateoasLinksTest extends AbstractApiTest {
    @Autowired EnrollmentModelAssembler enrollmentAssembler;

    @AfterEach
    void limpa() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void anonimoSoTemSelfEClasses() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(get("/classes/" + aula.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.classes.href").exists())
                .andExpect(jsonPath("$._links.enroll").doesNotExist())
                .andExpect(jsonPath("$._links.update").doesNotExist())
                .andExpect(jsonPath("$._links.delete").doesNotExist());
    }

    @Test
    void anonimoNaoVeCreateNaPagina() throws Exception {
        mvc.perform(get("/classes"))
                .andExpect(jsonPath("$._links.create").doesNotExist())
                .andExpect(jsonPath("$._embedded.classes[0]._links.enroll").doesNotExist());
    }

    @Test
    void userComVagaENaoInscritoTemEnrollMasNaoUpdateNemDelete() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(get("/classes/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(jsonPath("$._links.enroll.href").exists())
                .andExpect(jsonPath("$._links.update").doesNotExist())
                .andExpect(jsonPath("$._links.delete").doesNotExist());
        mvc.perform(get("/classes").header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(jsonPath("$._links.create").doesNotExist());
    }

    @Test
    void userJaInscritoNaoTemEnroll() throws Exception {
        GymClass aula = newClass(5);
        enroll("ana@fit.com", aula);
        mvc.perform(get("/classes/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(jsonPath("$._links.enroll").doesNotExist());
        mvc.perform(get("/classes/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, bruno()))
                .andExpect(jsonPath("$._links.enroll.href").exists());
    }

    @Test
    void aulaLotadaNaoTemEnrollParaNinguem() throws Exception {
        GymClass aula = newClass(1);
        enroll("bruno@fit.com", aula);
        mvc.perform(get("/classes/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(jsonPath("$._links.enroll").doesNotExist());
        mvc.perform(get("/classes/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath("$._links.enroll").doesNotExist());
    }

    @Test
    void adminTemUpdateDeleteECreateNaPagina() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(get("/classes/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists())
                .andExpect(jsonPath("$._links.enroll.href").exists());
        mvc.perform(get("/classes").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath("$._links.create.href").exists())
                .andExpect(jsonPath("$._embedded.classes[0]._links.update.href").exists());
        mvc.perform(get("/classes/search").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath("$._links.create.href").exists());
    }

    @Test
    void oLinkEnrollApontaParaOEndpointDeInscricao() throws Exception {
        GymClass aula = newClass(5);
        String href = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(get("/classes/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, ana()))
                        .andReturn().getResponse().getContentAsString(), "$._links.enroll.href");
        mvc.perform(post(java.net.URI.create(href)).header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(status().isCreated());
    }

    @Test
    void inscricaoTemClassECancelParaODono() throws Exception {
        GymClass aula = newClass(5);
        enroll("ana@fit.com", aula);
        mvc.perform(get("/enrollments/me").param("size", "100").header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.enrollments").isArray())
                .andExpect(jsonPath("$._embedded.enrollments[0]._links.class.href").exists())
                .andExpect(jsonPath("$._embedded.enrollments[0]._links.cancel.href").exists());
    }

    @Test
    void cancelSoParaDonoOuAdmin() {
        GymClass aula = newClass(5);
        Enrollment inscricao = enroll("ana@fit.com", aula);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("ana@fit.com", null, "ROLE_USER"));
        org.assertj.core.api.Assertions.assertThat(enrollmentAssembler.toModel(inscricao).hasLink("cancel")).isTrue();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("bruno@fit.com", null, "ROLE_USER"));
        EntityModel<EnrollmentResponse> outro = enrollmentAssembler.toModel(inscricao);
        org.assertj.core.api.Assertions.assertThat(outro.hasLink("cancel")).isFalse();
        org.assertj.core.api.Assertions.assertThat(outro.hasLink("class")).isTrue();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin@fit.com", null, "ROLE_ADMIN"));
        org.assertj.core.api.Assertions.assertThat(enrollmentAssembler.toModel(inscricao).hasLink("cancel")).isTrue();
        SecurityContextHolder.clearContext();
        org.assertj.core.api.Assertions.assertThat(enrollmentAssembler.toModel(inscricao).hasLink("cancel")).isFalse();
    }

    @Test
    void embeddedDeInscricoesEhEnrollments() throws Exception {
        enroll("ana@fit.com", newClass(5));
        mvc.perform(get("/enrollments/me").header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(jsonPath("$._embedded.enrollments").exists())
                .andExpect(jsonPath("$._embedded.classes").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").isNumber());
    }
}