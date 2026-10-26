package com.akash.sentinel_mcp;

import com.akash.sentinel.SentinelMcpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes =  SentinelMcpApplication.class)
class SentinelMcpApplicationTests {

	@Test
	void contextLoads() {
	}

}
