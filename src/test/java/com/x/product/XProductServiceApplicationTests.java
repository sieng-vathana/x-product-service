package com.x.product;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test requiring live database and redis services")
class XProductServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
