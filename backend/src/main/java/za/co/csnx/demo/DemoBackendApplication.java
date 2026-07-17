package za.co.csnx.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import za.co.csnx.engine.common.BaseRepositoryImpl;

// Component-scan the shared engine artifact (za.co.csnx.engine) alongside
// this module — registers the engine's ActivityRegistry, GrantEnforcer,
// GlobalExceptionHandler and JPA-auditing config. @EntityScan +
// @EnableJpaRepositories also include the engine package so the shared event
// admin viewers (outbox/inbox/error log entities + repositories) register here,
// letting the platform's Integration Trace read this module's ledgers over
// Channel 1; BaseRepository is @NoRepositoryBean so only concrete repos register.
@SpringBootApplication(scanBasePackages = {"za.co.csnx.demo", "za.co.csnx.engine"})
@ConfigurationPropertiesScan
@EntityScan(basePackages = {"za.co.csnx.demo", "za.co.csnx.engine"})
@EnableJpaRepositories(basePackages = {"za.co.csnx.demo", "za.co.csnx.engine"},
        repositoryBaseClass = BaseRepositoryImpl.class)
public class DemoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBackendApplication.class, args);
    }
}
