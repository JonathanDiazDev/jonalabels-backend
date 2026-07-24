package com.jonalabels;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "CLOUDINARY_URL=cloudinary://key:secret@cloud")
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
