package za.co.csnx.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import za.co.csnx.engine.common.BaseRepositoryImpl;

// Component-scan the shared engine artifact (za.co.csnx.engine) alongside
// this module — registers the engine's ActivityRegistry, GrantEnforcer,
// GlobalExceptionHandler and JPA-auditing config.
@SpringBootApplication(scanBasePackages = {"za.co.csnx.demo", "za.co.csnx.engine"})
@ConfigurationPropertiesScan
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl.class)
public class DemoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBackendApplication.class, args);
    }
}
