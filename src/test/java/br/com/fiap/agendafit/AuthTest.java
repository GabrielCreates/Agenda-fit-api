
package br.com.fiap.agendafit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AuthTest extends AbstractApiTest {

    @Autowired
    JwtDecoder jwtDecoder;

    @Value("${jwt.secret}")
    String secret;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final String REGISTER = """
            {"name":"Carlos Teste","email":"carlos@fit.com","password":"carlos123"}""";

    @Test
    void cadastroValidoDevolve201SemSenhaEComRoleUser() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Carlos Teste"))
                .andExpect(jsonPath("$.email").value("carlos@fit.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void senhaFicaGravadaComBCrypt() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER))
                .andExpect(status().isCreated());

        String hash = users.findByEmail("carlos@fit.com")
                .orElseThrow()
                .getPassword();

        Assertions.assertThat(hash).startsWith("$2").isNotEqualTo("carlos123");
        Assertions.assertThat(passwordEncoder.matches("carlos123", hash)).isTrue();
    }

    @Test
    void cadastroIgnoraRoleEnviadaPeloCliente() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Esperto","email":"esperto@fit.com","password":"senha123","role":"ADMIN"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void cadastroInvalidoDevolve400() throws Exception {
        for (String body : new String[]{
                "{}",
                """
                {"name":"","email":"a@b.com","password":"123456"}""",
                """
                {"name":"X","email":"nao-eh-email","password":"123456"}""",
                """
                {"name":"X","email":"x@fit.com","password":"123"}""",
                "isto não é json"
        }) {
            mvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").isString());
        }
    }

    @Test
    void emailDuplicadoDevolve409() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isString());

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Outra Ana","email":"ANA@fit.com","password":"ana12345"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void loginValidoDevolveTokenEExpiresIn() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@fit.com","password":"ana123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void loginAposCadastroFunciona() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"carlos@fit.com","password":"carlos123"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void loginInvalidoDevolve401() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@fit.com","password":"errada"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"fantasma@fit.com","password":"qualquer"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void loginSemCamposDevolve400() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenTemOsClaimsDoContratoEExpiraEmUmaHora() throws Exception {
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@fit.com","password":"admin123"}"""))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(body, "$.token");
        Jwt jwt = jwtDecoder.decode(token);

        Assertions.assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        Assertions.assertThat(jwt.getSubject()).isEqualTo("admin@fit.com");
        Assertions.assertThat(jwt.getClaimAsString("name")).isEqualTo("Administrador");
        Assertions.assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        Assertions.assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
                .isEqualTo(Duration.ofHours(1));
    }

    @Test
    void tokenValidoEhAceitoNoHeaderBearer() throws Exception {
        mvc.perform(get("/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, ana()))
                .andExpect(status().isOk());
    }

    @Test
    void semTokenEmRotaProtegidaDevolve401NoFormatoDoContrato() throws Exception {
        mvc.perform(get("/enrollments/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void tokenLixoDevolve401() throws Exception {
        mvc.perform(get("/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer isto.nao.eh.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void tokenInvalidoTambemEhRejeitadoEmRotaPublica() throws Exception {
        mvc.perform(get("/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer isto.nao.eh.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void tokenExpiradoDevolve401() throws Exception {
        Instant passado = Instant.now().minus(Duration.ofHours(2));
        String token = sign(secret, passado, passado.plusSeconds(3600));

        mvc.perform(get("/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void tokenAssinadoComOutraChaveDevolve401() throws Exception {
        Instant agora = Instant.now();
        String token = sign(
                "outra-chave-secreta-com-mais-de-32-caracteres!",
                agora,
                agora.plusSeconds(3600));

        mvc.perform(get("/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void tokenAdulteradoDevolve401() throws Exception {
        String token = ana().substring("Bearer ".length());
        String[] parts = token.split("\\.");

        String payload = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
        ).replace("\"USER\"", "\"ADMIN\"");

        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        mvc.perform(post("/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLASS_JSON))
                .andExpect(status().isUnauthorized());
    }

    private String sign(String signingSecret, Instant issuedAt, Instant expiresAt) {
        var secretKey = new SecretKeySpec(
                signingSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("ana@fit.com")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("name", "Ana Souza")
                .claim("role", "USER")
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }
}
