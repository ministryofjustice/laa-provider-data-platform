package uk.gov.justice.laa.providerdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the Spring Boot microservice application. */
@SpringBootApplication
public class ProviderDataServiceApplication {

  /**
   * The application main method.
   *
   * @param args the application arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(ProviderDataServiceApplication.class, args);
  }
}
