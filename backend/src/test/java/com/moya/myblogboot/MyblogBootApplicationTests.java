package com.moya.myblogboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MyblogBootApplicationTests extends AbstractContainerBaseTest {

	@Test
	void contextLoads() {
	}
}
