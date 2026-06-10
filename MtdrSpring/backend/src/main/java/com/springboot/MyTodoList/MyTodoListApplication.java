package com.springboot.MyTodoList;

import com.springboot.MyTodoList.config.BotProps;
import com.springboot.MyTodoList.config.DeepSeekConfig;
import com.springboot.MyTodoList.service.SprintService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableConfigurationProperties(BotProps.class)
@Import(DeepSeekConfig.class)
public class MyTodoListApplication {

	private static final Logger logger = LoggerFactory.getLogger(MyTodoListApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(MyTodoListApplication.class, args);
	}

	@Bean
	public ApplicationRunner initializeSprints(SprintService sprintService) {
		return args -> {
			// TEMPORARY: automatic sprint seeding disabled.
			// Keep this block commented for potential removal or controlled reactivation.
			logger.info("Automatic sprint initialization is disabled.");

			/*
			try {
				java.util.List<com.springboot.MyTodoList.model.Sprint> existingSprints = sprintService.findAll();

				// Crear Sprint 2 si no existe
				if (existingSprints.stream().noneMatch(s -> s.getName().equals("Sprint 2"))) {
					logger.info("Creating Sprint 2...");
					SprintRequestDTO sprint2 = new SprintRequestDTO();
					sprint2.setName("Sprint 2");
					sprint2.setStartDate(LocalDateTime.of(2026, 4, 22, 9, 0));
					sprint2.setEndDate(LocalDateTime.of(2026, 4, 28, 18, 0));
					sprintService.createSprint(sprint2);
				}

				// Crear Sprint 3 si no existe
				if (existingSprints.stream().noneMatch(s -> s.getName().equals("Sprint 3"))) {
					logger.info("Creating Sprint 3...");
					SprintRequestDTO sprint3 = new SprintRequestDTO();
					sprint3.setName("Sprint 3");
					sprint3.setStartDate(LocalDateTime.of(2026, 4, 29, 9, 0));
					sprint3.setEndDate(LocalDateTime.of(2026, 5, 5, 18, 0));
					sprintService.createSprint(sprint3);
				}

				// Crear Sprint 4 si no existe
				if (existingSprints.stream().noneMatch(s -> s.getName().equals("Sprint 4"))) {
					logger.info("Creating Sprint 4...");
					SprintRequestDTO sprint4 = new SprintRequestDTO();
					sprint4.setName("Sprint 4");
					sprint4.setStartDate(LocalDateTime.of(2026, 5, 6, 9, 0));
					sprint4.setEndDate(LocalDateTime.of(2026, 5, 12, 18, 0));
					sprintService.createSprint(sprint4);
				}

				logger.info("Sprint initialization completed.");
			} catch (Exception e) {
				logger.error("Error initializing sprints: " + e.getMessage(), e);
			}
			*/
		};
	}

}
