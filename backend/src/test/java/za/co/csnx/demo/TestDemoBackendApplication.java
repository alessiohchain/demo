package za.co.csnx.demo;

import org.springframework.boot.SpringApplication;

public class TestDemoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(DemoBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
