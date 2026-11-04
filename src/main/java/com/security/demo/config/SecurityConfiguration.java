package com.security.demo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// SpringBootWebSecurityConfiguration.java
@Configuration
@EnableWebSecurity
/*
1. prePostEnabled = true: Enables Spring Security's @PreAuthorize and @PostAuthorize annotations
 for method-level security checks before and after method execution.

2.securedEnabled = true: Enables support for the @Secured annotation,
 allowing role-based access control on methods.

3. jsr250Enabled = true: Enables support for JSR-250 annotations like @RolesAllowed,
 providing standard Java role-based security on methods.
 */
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(requests ->
        requests
            .requestMatchers("/private/**")
            .authenticated()
            .requestMatchers("/public/**")
            .permitAll()
            .requestMatchers("/restricted/**")
            .denyAll()
            .anyRequest()
            .authenticated());
    http.sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.httpBasic(withDefaults());
    return http.build();
  }

  /*
  {noop} is a password encoder prefix in Spring Security that means
   "no operation"—the password is stored and compared as plain text without any encoding or hashing.
   */
  @Bean
  public UserDetailsService userDetailsService() {
    final InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    if (!manager.userExists("user")) {
      manager.createUser(
          User.withUsername("user").password("{noop}userpassword").roles("USER").build());
    }
    if (!manager.userExists("admin")) {
      manager.createUser(
          User.withUsername("admin").password("{noop}adminpassword").roles("ADMIN").build());
    }
    return manager;
  }
}
