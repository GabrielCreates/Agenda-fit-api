package br.com.fiap.agendafit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.agendafit.model.GymClass;
import br.com.fiap.agendafit.model.Level;
import br.com.fiap.agendafit.model.Modality;
import org.junit.jupiter.api.Test;

class QueryByExampleTest extends AbstractApiTest {
    private long count(java.util.function.Predicate<GymClass> filter) {
        return classes.findAll().stream().filter(filter).count();
    }

    @Test
    void semFiltrosDevolveTodasPaginadas() throws Exception {
        mvc.perform(get("/classes/example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(60))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$._embedded.classes").isArray());
    }

    @Test
    void filtroIsoladoPorModalidade() throws Exception {
        mvc.perform(get("/classes/example").param("modality", "YOGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(count(c -> c.getModality() == Modality.YOGA)))
                .andExpect(jsonPath("$._embedded.classes[?(@.modality != 'YOGA')]").isEmpty());
    }

    @Test
    void filtroIsoladoPorNivel() throws Exception {
        mvc.perform(get("/classes/example").param("level", "ADVANCED"))
                .andExpect(jsonPath("$.page.totalElements").value(count(c -> c.getLevel() == Level.ADVANCED)))
                .andExpect(jsonPath("$._embedded.classes[?(@.level != 'ADVANCED')]").isEmpty());
    }

    @Test
    void filtroIsoladoPorInstrutorIgnoraCaixa() throws Exception {
        long esperado = count(c -> c.getInstructor().equalsIgnoreCase("Carla Mendes"));
        mvc.perform(get("/classes/example").param("instructor", "carla mendes"))
                .andExpect(jsonPath("$.page.totalElements").value(esperado));
        mvc.perform(get("/classes/example").param("instructor", "CARLA MENDES"))
                .andExpect(jsonPath("$.page.totalElements").value(esperado));
    }

    @Test
    void instrutorIncompletoNaoCasa() throws Exception {
        mvc.perform(get("/classes/example").param("instructor", "carla"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void filtrosCombinados() throws Exception {
        long esperado = count(c -> c.getModality() == Modality.YOGA && c.getLevel() == Level.BEGINNER
                && c.getInstructor().equalsIgnoreCase("Carla Mendes"));
        mvc.perform(get("/classes/example")
                        .param("modality", "YOGA").param("level", "BEGINNER").param("instructor", "carla mendes")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(esperado));
    }

    @Test
    void filtroEmBrancoEhIgnorado() throws Exception {
        mvc.perform(get("/classes/example").param("instructor", " "))
                .andExpect(jsonPath("$.page.totalElements").value(60));
    }

    @Test
    void linksDeNavegacaoMantemOsFiltros() throws Exception {
        mvc.perform(get("/classes/example").param("modality", "YOGA").param("size", "2"))
                .andExpect(jsonPath("$._links.next.href", containsString("modality=YOGA")))
                .andExpect(jsonPath("$._links.last.href", containsString("modality=YOGA")));
    }

    @Test
    void valorDeEnumInvalidoDevolve400() throws Exception {
        mvc.perform(get("/classes/example").param("modality", "BOXE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void resultadoVazioNaoTemEmbedded() throws Exception {
        mvc.perform(get("/classes/example").param("instructor", "ninguem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }
}