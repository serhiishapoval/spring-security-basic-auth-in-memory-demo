# Spring Security: HTTP Basic Authentication with In-Memory Users

A small Spring Boot demo of **HTTP Basic authentication** with two in-memory users, path-based access rules, and role checks at the method level. It shows how the same role rule can be written as a URL matcher, as `@PreAuthorize`, or as `@Secured`.

The project is a learning demo. It stores passwords in plain text and is not meant for production. See [Known limitations](#known-limitations).

---

## Table of contents

- [Features](#features)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Users](#users)
- [Endpoints and access rules](#endpoints-and-access-rules)
- [Running locally](#running-locally)
- [Running the tests](#running-the-tests)
- [Trying it out](#trying-it-out)
- [How security is configured](#how-security-is-configured)
- [Build notes](#build-notes)
- [Known limitations](#known-limitations)

---

## Features

- HTTP Basic authentication (`httpBasic`). Credentials are sent on every request.
- Stateless sessions. The server does not create an HTTP session (`SessionCreationPolicy.STATELESS`).
- Two in-memory users, `user` (role `USER`) and `admin` (role `ADMIN`), created at startup.
- URL-based rules for `/public/**`, `/private/**` and `/restricted/**`.
- Method-level security through `@PreAuthorize`, `@Secured` and JSR-250 annotations, all enabled.
- Debug logging for `org.springframework.security`, so you can see each filter and decision in the log.

---

## Project structure

```
spring-security-basic-auth-in-memory-demo/
├── pom.xml                                       # Spring Boot 3.5.7 parent, Java 27 / release 25
├── .idea/                                        # IntelliJ settings (JDK 27, google-java-format enabled)
└── src/main/
    ├── java/com/security/demo/
    │   ├── SpringSecurityFormBasicAuthInMemoryDemoApplication.java   # Spring Boot entry point
    │   ├── config/
    │   │   └── SecurityConfiguration.java        # filter chain, in-memory users, method security
    │   ├── controller/
    │   │   └── SpringSecurityTestController.java # endpoints with different access rules
    │   └── service/
    │       ├── UserDetailsServiceSecure.java     # interface
    │       └── impl/UserDetailsServiceSecureImpl.java  # @Secured method example
    └── resources/
        └── application.properties                # app name, DEBUG logging for Spring Security
```

The entry point class keeps the name `SpringSecurityFormBasicAuthInMemoryDemoApplication` from an earlier version of the project. The application uses HTTP Basic, not form login.

---

## Tech stack

- Java 27 (`java.version`), compiled with `maven.compiler.release` **25**
- Spring Boot 3.5.7 (parent POM)
- Spring Boot Starter Security and Starter Web
- JUnit 6.1.3 and Mockito 5.24.0 for unit tests (pinned in `pom.xml`, included through `spring-boot-starter-test`)
- Spring Security Test (test scope, declared but not used by the current tests)
- Maven

---

## Users

Both users are created in `SecurityConfiguration.userDetailsService()`. Passwords use the `{noop}` prefix, which means they are stored and compared in plain text.

| Username | Password | Roles |
|---|---|---|
| `user` | `userpassword` | `USER` |
| `admin` | `adminpassword` | `ADMIN` |

There is no registration or password change. The users exist only while the application is running.

---

## Endpoints and access rules

The server listens on the default port **8080**. Every request that needs authentication must send an `Authorization: Basic` header. The examples use curl's `-u user:password`.

| Method | Path | URL rule | Method rule | Who can call it |
|---|---|---|---|---|
| `GET` | `/public/information` | `permitAll` | – | Anyone, no credentials |
| `GET` | `/private/information` | `authenticated` | – | Any authenticated user |
| `GET` | `/restricted/information` | `denyAll` | – | Nobody (see below) |
| `GET` | `/user/details` | `authenticated` (default) | `@PreAuthorize("hasRole('USER')")` | `USER` only |
| `GET` | `/user/details/method` | `authenticated` (default) | `@Secured("ROLE_USER")` on the service method | `USER` only |
| `GET` | `/admin/details` | `authenticated` (default) | `@PreAuthorize("hasRole('ADMIN')")` | `ADMIN` only |
| `GET` | `/user-admin/details` | `authenticated` (default) | `@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")` | `USER` or `ADMIN`. Returns the current username |

URL rules are checked first. A method rule runs only if the URL rule allowed the request. A request that fails a method rule gets `403 Forbidden`.

`/restricted/information` is blocked by `denyAll`, so no user can reach it. An anonymous caller gets `401`, and an authenticated caller gets `403`.

Responses are plain text.

---

## Running locally

### Prerequisites

- **JDK 27** (or 25 or later, see [Build notes](#build-notes))
- **Maven 3.6.3+**

### Build and run

```bash
mvn clean install
mvn spring-boot:run
```

The application starts on http://localhost:8080.

---

## Running the tests

```bash
mvn test
```

The unit tests live in `src/test/java`. `SecurityConfigurationTest` checks the two in-memory users: their passwords, their roles, and the rejection of an unknown user. The tests start no server and need no network access.

The endpoint access rules in the table above are not covered by these tests. Checking them needs a `MockMvc` test that runs the full security filter chain, and the project does not have one yet.

---

## Trying it out

Public endpoint, no credentials:

```bash
curl http://localhost:8080/public/information
```

```
Public information endpoint
```

Private endpoint, anonymous. The server returns `401`:

```bash
curl -i http://localhost:8080/private/information
```

Private endpoint, with credentials:

```bash
curl -u user:userpassword http://localhost:8080/private/information
```

Role checks:

```bash
# USER gets the user endpoints
curl -u user:userpassword http://localhost:8080/user/details              # 200
curl -u user:userpassword http://localhost:8080/user/details/method       # 200

# USER is refused on the admin endpoint
curl -i -u user:userpassword http://localhost:8080/admin/details          # 403

# ADMIN gets the admin endpoint, and is refused on the user-only endpoints
curl -u admin:adminpassword http://localhost:8080/admin/details           # 200
curl -i -u admin:adminpassword http://localhost:8080/user/details         # 403
curl -i -u admin:adminpassword http://localhost:8080/user/details/method  # 403

# Both roles can use the combined endpoint
curl -u user:userpassword  http://localhost:8080/user-admin/details  # ... Current User: user
curl -u admin:adminpassword http://localhost:8080/user-admin/details # ... Current User: admin

# Nobody can use the restricted endpoint
curl -i -u admin:adminpassword http://localhost:8080/restricted/information  # 403
```

With `logging.level.org.springframework.security=DEBUG`, the console shows each security filter and the decision it made for each request.

---

## How security is configured

`SecurityConfiguration` has three parts.

**1. Filter chain** (`defaultSecurityFilterChain`)

- `requestMatchers("/private/**").authenticated()`
- `requestMatchers("/public/**").permitAll()`
- `requestMatchers("/restricted/**").denyAll()`
- `anyRequest().authenticated()` for everything else
- `sessionManagement(... STATELESS)`: no session cookie, so each request is authenticated on its own
- `httpBasic(withDefaults())`: HTTP Basic is the only login mechanism

**2. Method security** (`@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)`)

- `@PreAuthorize` and `@PostAuthorize` (SpEL expressions)
- `@Secured` (role names, the `ROLE_` prefix is required)
- `@RolesAllowed` (JSR-250). It is enabled but not used in the code.

**3. In-memory users** (`userDetailsService()`)

An `InMemoryUserDetailsManager` with the two users above. The `userExists` checks are redundant, because the manager starts empty.

---

## Build notes

- `java.version` is 27. The compiler targets `maven.compiler.release` **25**. Spring Boot's repackage plugin cannot read Java 27 class files (major version 71), so the build must target 25.
- The project has no Lombok dependency.
- Sources follow google-java-format style (IntelliJ setting in `.idea/google-java-format.xml`).
- Unit tests use JUnit 6 and Mockito 5. Their versions are pinned in `pom.xml` (`junit-jupiter.version`, `mockito.version`). `byte-buddy.version` is pinned to 1.18.17 so that Mockito can mock classes on Java 27.
- There are no integration tests. `spring-security-test` is on the test classpath for future use.

---

## Known limitations

This is a demo. The following are known gaps.

- **Plain-text passwords.** `{noop}` stores passwords without hashing. Use `BCryptPasswordEncoder` with `{bcrypt}` hashes for anything real.
- **Basic credentials on every request.** HTTP Basic sends the username and password with each call, and it is safe only over HTTPS. The app does not enable TLS.
- **Hard-coded users.** The users and passwords are in source code. They cannot be changed without a restart and a code change.
- **Misleading class name.** `SpringSecurityFormBasicAuthInMemoryDemoApplication` is left over from an earlier version. The app does not use form login.
- **Redundant checks.** `userDetailsService()` checks `userExists` before each `createUser`, but the manager is new and empty, so the checks have no effect.
- **Unused annotation.** `@RolesAllowed` (JSR-250) is enabled but no method uses it.
- **Mixed role syntax.** `hasRole('USER')` in `@PreAuthorize` and `@Secured("ROLE_USER")` both work, but they use different conventions (the `ROLE_` prefix is added automatically in one case and must be written in the other).
- **Few tests.** One unit test class covers the in-memory users. The endpoint access rules and method security are not checked by automated tests, so they are verified only by running the app.
