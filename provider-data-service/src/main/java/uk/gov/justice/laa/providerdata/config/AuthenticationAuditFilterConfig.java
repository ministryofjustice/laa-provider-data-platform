package uk.gov.justice.laa.providerdata.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import uk.gov.justice.laa.providerdata.security.AuthenticationAuditFilter;
import uk.gov.justice.laa.providerdata.security.AuthenticationAuditLogger;

/** Registers request audit logging after authentication has been attempted by security filters. */
@Configuration
public class AuthenticationAuditFilterConfig {

  /** Creates and orders the audit filter for every incoming request. */
  @Bean
  public FilterRegistrationBean<AuthenticationAuditFilter> authenticationAuditFilterRegistration(
      AuthenticationAuditLogger authenticationAuditLogger) {
    FilterRegistrationBean<AuthenticationAuditFilter> filterRegistration =
        new FilterRegistrationBean<>(new AuthenticationAuditFilter(authenticationAuditLogger));
    filterRegistration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return filterRegistration;
  }
}
