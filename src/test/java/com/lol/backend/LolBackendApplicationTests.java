package com.lol.backend;

import com.lol.backend.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class LolBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
