package com.vikas.studentperformancedossier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
// @EnableJpaAuditing: activates Spring Data JPA auditing so @CreatedDate/@LastModifiedDate fields (see BaseEntity) get populated automatically.
@EnableJpaAuditing
public class StudentPerformanceDossierApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentPerformanceDossierApplication.class, args);
	}

}
