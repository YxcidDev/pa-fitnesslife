package com.fitnesslife.gym;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requiere Mongo, Redis y servicios de IA; no disponible en CI")
class GymApplicationTests {

	@Test
	void contextLoads() {
	}
}