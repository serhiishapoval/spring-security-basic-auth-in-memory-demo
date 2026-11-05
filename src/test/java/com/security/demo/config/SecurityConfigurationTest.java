package com.security.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class SecurityConfigurationTest {

  private final UserDetailsService userDetailsService =
      new SecurityConfiguration().userDetailsService();

  @ParameterizedTest(name = "in-memory user [{0}] has role [{2}]")
  @CsvSource({"user, {noop}userpassword, USER", "admin, {noop}adminpassword, ADMIN"})
  void givenInMemoryUser_whenLoadUserByUsername_thenReturnsUserWithPasswordAndRole(
      final String username, final String password, final String role) {
    // given
    final UserDetails expected = User.withUsername(username).password(password).roles(role).build();

    // when
    final UserDetails result = userDetailsService.loadUserByUsername(username);

    // then
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void givenUnknownUser_whenLoadUserByUsername_thenThrowsUsernameNotFoundException() {
    // given

    // when / then
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
