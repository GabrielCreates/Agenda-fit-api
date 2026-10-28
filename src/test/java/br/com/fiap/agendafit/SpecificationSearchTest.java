package br.com.fiap.agendafit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.function.Predicate;
import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.model.Level;
import br.com.fiap.agendafit.model.Modality;
import org.junit.jupiter.api.Test;

class SpecificationSearchTest extends AbstractApiTest {
    private long count(Predicate<GymClass> filter) {
        return classes.findAll().stream().filter(filter).count();
    }

    private boolean hasSpot(GymClass c) {
        return enrollments.countByGymClassId(c.getId()) < c.getCapacity();
    }

    @Test
    void semFiltrosDevolveTudo() throws Exception {
        mvc.perform(get("/classes/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(60))
                .andExpect(jsonPath("$.page.size").value(10));
    }

    @Test
    void titleContemSemDiferenciarCaixa() throws Exception {
        long esperado = count(c -> c.getTitle().toLowerCase().contains("yoga"));
        mvc.perform(get("/classes/search").param("title", "YoGa"))
                .andExpect(jsonPath("$.page.totalElements").value(esperado));
        mvc.perform(get("/classes/search").param("title", "amanhecer"))
                .andExpect(jsonPath("$.page.totalElements").value(count(c -> c.getTitle().contains("amanhecer"))));
    }

    @Test
    void titleComCaracterEspecialNaoViraCuringa() throws Exception {
        mvc.perform(get("/classes/search").param("title", "%"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void modalityIgual() throws Exception {
        mvc.perform(get("/classes/search").param("modality", "SPINNING"))
                .andExpect(jsonPath("$.page.totalElements").value(count(c -> c.getModality() == Modality.SPINNING)));
    }

    @Test
    void levelIgual() throws Exception {
        mvc.perform(get("/classes/search").param("level", "INTERMEDIATE"))
                .andExpect(jsonPath("$.page.totalElements").value(count(c -> c.getLevel() == Level.INTERMEDIATE)));
    }

    @Test
    void startsAfterEStartsBeforeSaoInclusivos() throws Exception {
        LocalDateTime t = LocalDateTime.of(2030, 6, 1, 9, 0);
        newClass(5, t.minusSeconds(1));
        newClass(5, t);
        newClass(5, t.plusSeconds(1));
        mvc.perform(get("/classes/search").param("startsAfter", t.toString()).param("startsBefore", "2031-01-01T00:00:00"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/classes/search").param("startsAfter", "2030-01-01T00:00:00").param("startsBefore", t.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/classes/search").param("startsAfter", t.toString()).param("startsBefore", t.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/classes/search").param("startsAfter", t.plusSeconds(2).toString()))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void availableOnlyExcluiAulasLotadasUsandoSubquery() throws Exception {
        long comVaga = count(this::hasSpot);
        long lotadas = 60 - comVaga;
        org.assertj.core.api.Assertions.assertThat(lotadas).as("DataLoader cria aulas lotadas").isPositive();
        mvc.perform(get("/classes/search").param("availableOnly", "true").param("size", "100"))
                .andExpect(jsonPath("$.page.totalElements").value(comVaga))
                .andExpect(jsonPath("$._embedded.classes[?(@.availableSpots == 0)]").isEmpty());
    }

    @Test
    void availableOnlyFalseOuAusenteNaoFiltra() throws Exception {
        mvc.perform(get("/classes/search").param("availableOnly", "false"))
                .andExpect(jsonPath("$.page.totalElements").value(60));
    }

    @Test
    void aulaComUmaVagaRestanteApareceEQuandoLotaSai() throws Exception {
        GymClass aula = newClass(2);
        enroll("ana@fit.com", aula);
        mvc.perform(get("/classes/search").param("availableOnly", "true").param("startsAfter", "2030-01-01T00:00:00"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        enroll("bruno@fit.com", aula);
        mvc.perform(get("/classes/search").param("availableOnly", "true").param("startsAfter", "2030-01-01T00:00:00"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void filtrosCombinadosComAnd() throws Exception {
        long esperado = count(c -> c.getModality() == Modality.YOGA && c.getLevel() == Level.BEGINNER
                && c.getTitle().toLowerCase().contains("yoga") && hasSpot(c));
        mvc.perform(get("/classes/search")
                        .param("title", "yoga").param("modality", "YOGA").param("level", "BEGINNER")
                        .param("availableOnly", "true").param("size", "100"))
                .andExpect(jsonPath("$.page.totalElements").value(esperado));
    }

    @Test
    void combinacaoSemResultadoNaoTemEmbedded() throws Exception {
        mvc.perform(get("/classes/search").param("title", "yoga").param("modality", "DANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void paginacaoELinksPreservamOsFiltros() throws Exception {
        mvc.perform(get("/classes/search").param("modality", "YOGA").param("availableOnly", "true").param("size", "2"))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$._links.next.href", containsString("modality=YOGA")))
                .andExpect(jsonPath("$._links.next.href", containsString("availableOnly=true")))
                .andExpect(jsonPath("$._links.self.href", containsString("modality=YOGA")));
    }

    @Test
    void dataInvalidaDevolve400() throws Exception {
        mvc.perform(get("/classes/search").param("startsAfter", "ontem"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }
}