package com.vikas.studentperformancedossier.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Kept separate from the main application class so that @WebMvcTest and other
// slice tests (which exclude JPA auto-configuration) don't try to process
// @EnableJpaAuditing and fail wiring jpaAuditingHandler/jpaMappingContext.
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
