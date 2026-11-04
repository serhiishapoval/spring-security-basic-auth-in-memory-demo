package com.security.demo.controller;

import com.security.demo.service.UserDetailsServiceSecure;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpringSecurityTestController {

  private final UserDetailsServiceSecure userDetailsServiceSecure;

  public SpringSecurityTestController(UserDetailsServiceSecure userDetailsServiceSecure) {
    this.userDetailsServiceSecure = userDetailsServiceSecure;
  }

  @GetMapping("/restricted/information")
  public String restrictedInformation() {
    return "Restricted information endpoint";
  }

  @PreAuthorize("hasRole('USER')")
  @GetMapping("/user/details")
  public String getUserDetails() {
    return "User details endpoint";
  }

  @GetMapping("/user/details/method")
  public String getUserDetailsMethodLevelSecurity() {
    return this.userDetailsServiceSecure.getUserDetailsMethodLevelSecurity();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin/details")
  public String getAdminDetails() {
    return "Admin details endpoint";
  }

  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  // @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
  @GetMapping("/user-admin/details")
  public String getUserAndAdminDetails(@AuthenticationPrincipal final UserDetails userDetails) {
    return "User-Admin details endpoint. Current User: " + userDetails.getUsername();
  }

  @GetMapping("/private/information")
  public String privateInformation() {
    return "Private information endpoint";
  }

  @GetMapping("/public/information")
  public String publicInformation() {
    return "Public information endpoint";
  }
}
