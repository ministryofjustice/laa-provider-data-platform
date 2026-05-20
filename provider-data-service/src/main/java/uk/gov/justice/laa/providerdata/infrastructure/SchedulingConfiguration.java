package uk.gov.justice.laa.providerdata.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables scheduled infrastructure jobs such as outbox dispatching. */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {}

