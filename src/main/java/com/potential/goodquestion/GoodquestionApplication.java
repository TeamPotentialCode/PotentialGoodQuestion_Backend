package com.potential.goodquestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // BaseEntity의 createdAt / updatedAt 자동 관리
public class GoodquestionApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoodquestionApplication.class, args);
	}

}
