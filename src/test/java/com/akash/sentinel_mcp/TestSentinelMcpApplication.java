package com.akash.sentinel_mcp;

import com.akash.sentinel.SentinelMcpApplication;
import org.springframework.boot.SpringApplication;

public class TestSentinelMcpApplication {

	public static void main(String[] args) {
		SpringApplication.from(SentinelMcpApplication::main).with(TestcontainersConfiguration.class).run(args);
	}
}
