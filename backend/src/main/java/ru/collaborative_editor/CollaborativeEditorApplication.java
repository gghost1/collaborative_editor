package ru.collaborative_editor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CollaborativeEditorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CollaborativeEditorApplication.class, args);
	}

}
