package br.com.fiap.agendafit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.agendafit.model.GymClass;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class SecurityRulesTest extends AbstractApiTest {

    private static final String AUTH = HttpHeaders.AUTHORIZATION;

    @Test
    void consultasDeAulasSaoPublicas() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(get("/classes")).andExpect(status().isOk());
        mvc.perform(get("/classes/example")).andExpect(status().isOk());
        mvc.perform(get("/classes/search")).andExpect(status().isOk());
        mvc.perform(get("/classes/" + aula.getId())).andExpect(status().isOk());
    }

    @Test
    void anonimoNaoGerenciaAulas() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(post("/classes").contentType(MediaType.APPLICATION_JSON).content(CLASS_JSON))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").isString());
        mvc.perform(put("/classes/" + aula.getId()).contentType(MediaType.APPLICATION_JSON).content(CLASS_JSON))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").isString());
        mvc.perform(delete("/classes/" + aula.getId()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").isString());
    }

    @Test
    void userComumRecebe403NasOperacoesAdministrativas() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(post("/classes").header(AUTH, ana()).contentType(MediaType.APPLICATION_JSON).content(CLASS_JSON))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").isString());
        mvc.perform(put("/classes/" + aula.getId()).header(AUTH, ana()).contentType(MediaType.APPLICATION_JSON).content(CLASS_JSON))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").isString());
        mvc.perform(delete("/classes/" + aula.getId()).header(AUTH, ana()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").isString());
        mvc.perform(get("/classes/" + aula.getId())).andExpect(status().isOk());
    }

    @Test
    void adminCriaComLocationEAulaPassaAExistir() throws Exception {
        String location = mvc.perform(post("/classes").header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(CLASS_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.availableSpots").value(15))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(java.net.URI.create(location))).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Funcional ao ar livre"));
    }

    @Test
    void adminCriaComDadosInvalidosRecebe400() throws Exception {
        mvc.perform(post("/classes").header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"modality\":\"YOGA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void adminAtualiza200E404() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(put("/classes/" + aula.getId()).header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(CLASS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aula.getId()))
                .andExpect(jsonPath("$.title").value("Funcional ao ar livre"))
                .andExpect(jsonPath("$.capacity").value(15));
        mvc.perform(put("/classes/999999").header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(CLASS_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Class not found"));
        mvc.perform(put("/classes/" + aula.getId()).header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminExclui204E404() throws Exception {
        GymClass aula = newClass(5);
        enroll("ana@fit.com", aula);
        mvc.perform(delete("/classes/" + aula.getId()).header(AUTH, admin())).andExpect(status().isNoContent());
        mvc.perform(get("/classes/" + aula.getId())).andExpect(status().isNotFound());
        mvc.perform(delete("/classes/" + aula.getId()).header(AUTH, admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isString());
    }


    @Test
    void inscreverEMinhasInscricoesExigemLogin() throws Exception {
        GymClass aula = newClass(5);
        mvc.perform(post("/classes/" + aula.getId() + "/enrollments"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").isString());
        mvc.perform(get("/enrollments/me"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").isString());
        mvc.perform(delete("/enrollments/1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").isString());
    }

    @Test
    void swaggerEstaLiberadoSemLogin() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void documentacaoRefleteAApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.info.title").value("Agenda Fit API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.info.description").isString())
                .andExpect(jsonPath("$.paths['/classes'].get.summary").isString())
                .andExpect(jsonPath("$.paths['/classes/example'].get.summary").isString())
                .andExpect(jsonPath("$.paths['/classes/search'].get.parameters[?(@.name == 'availableOnly')].description").isNotEmpty())
                .andExpect(jsonPath("$.paths['/classes/search'].get.parameters[?(@.name == 'startsAfter')].description").isNotEmpty())
                .andExpect(jsonPath("$.paths['/classes'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/classes/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/classes/{id}/enrollments'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/auth/register'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/enrollments/me'].get.summary").isString())
                .andExpect(jsonPath("$.paths['/enrollments/{id}'].delete.responses['403']").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Aulas')]").isNotEmpty());
    }

    @Test
    void preflightCorsNaoExigeToken() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/enrollments/me")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk());
    }
}
