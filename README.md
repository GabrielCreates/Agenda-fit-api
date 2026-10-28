# Agenda Fit API

API REST de agenda de academia (Projeto Diamante, Java Advanced). Grade de aulas paginada, buscas com Query by Example e Specification, HATEOAS com links condicionais, documentação Swagger e segurança JWT com perfis USER e ADMIN.

## Integrantes

- Gabriel Rocha — RM567023
- Luis Fillipe Rodrigues — RM567918
- Luiz Felipe Kichimoto — RM567726

## Tecnologias

Java 17, Spring Boot, Spring Data JPA, Spring Security (OAuth2 Resource Server + JWT HS256), Spring HATEOAS, springdoc (Swagger) e banco H2 em memória.

## Como executar

Não é necessária nenhuma configuração extra.

```bash
git clone LINK_DO_REPOSITORIO
cd PASTA_DO_REPOSITORIO
mvn spring-boot:run
```

A API sobe em http://localhost:8080

## Documentação

Swagger UI: http://localhost:8080/swagger-ui.html

## Usuários de teste

| E-mail | Senha | Perfil |
|---|---|---|
| admin@fit.com | admin123 | ADMIN |
| ana@fit.com | ana123 | USER |
| bruno@fit.com | bruno123 | USER |

## Autenticação

Faça `POST /auth/login` e envie o token nas rotas protegidas:

```
Authorization: Bearer <token>
```

## Testes

```bash
mvn test
```