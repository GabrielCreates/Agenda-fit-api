package br.com.fiap.agendafit;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class PaginationTest extends AbstractApiTest {
    @Test
    void semParametrosDevolvePagina0Com10AulasOrdenadasPorHorario() throws Exception {
        String body = mvc.perform(get("/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.classes", hasSize(10)))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(60))
                .andExpect(jsonPath("$.page.totalPages").value(6))
                .andExpect(jsonPath("$.page.number").value(0))
                .andReturn().getResponse().getContentAsString();
        List<String> starts = JsonPath.read(body, "$._embedded.classes[*].startsAt");
        List<LocalDateTime> parsed = starts.stream().map(LocalDateTime::parse).toList();
        org.assertj.core.api.Assertions.assertThat(parsed).isSorted();
    }

    @Test
    void pageESizeFuncionam() throws Exception {
        mvc.perform(get("/classes").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.classes", hasSize(5)))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(12))
                .andExpect(jsonPath("$._links.first.href").exists())
                .andExpect(jsonPath("$._links.prev.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists());
    }

    @Test
    void primeiraPaginaNaoTemPrevEUltimaNaoTemNext() throws Exception {
        mvc.perform(get("/classes"))
                .andExpect(jsonPath("$._links.prev").doesNotExist())
                .andExpect(jsonPath("$._links.next.href").exists());
        mvc.perform(get("/classes").param("page", "5"))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                .andExpect(jsonPath("$._links.prev.href").exists());
    }

    @Test
    void paginaForaDoIntervaloNaoTemEmbedded() throws Exception {
        mvc.perform(get("/classes").param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(60));
    }

    @Test
    void aulaSegueOContratoJson() throws Exception {
        mvc.perform(get("/classes").param("size", "1"))
                .andExpect(jsonPath("$._embedded.classes[0].id").isNumber())
                .andExpect(jsonPath("$._embedded.classes[0].title").isString())
                .andExpect(jsonPath("$._embedded.classes[0].modality").isString())
                .andExpect(jsonPath("$._embedded.classes[0].level").isString())
                .andExpect(jsonPath("$._embedded.classes[0].instructor").isString())
                .andExpect(jsonPath("$._embedded.classes[0].startsAt").isString())
                .andExpect(jsonPath("$._embedded.classes[0].durationMinutes").isNumber())
                .andExpect(jsonPath("$._embedded.classes[0].capacity").isNumber())
                .andExpect(jsonPath("$._embedded.classes[0].availableSpots").isNumber())
                .andExpect(jsonPath("$._embedded.classes[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.classes[0]._links.classes.href").exists());
    }

    @Test
    void availableSpotsDescontaInscritos() throws Exception {
        var aula = newClass(5);
        enroll("ana@fit.com", aula);
        enroll("bruno@fit.com", aula);
        mvc.perform(get("/classes/" + aula.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.availableSpots").value(3));
    }

    @Test
    void aulaInexistenteDevolve404NoFormatoDoContrato() throws Exception {
        mvc.perform(get("/classes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Class not found"));
    }

    @Test
    void idInvalidoDevolve400() throws Exception {
        mvc.perform(get("/classes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }
}