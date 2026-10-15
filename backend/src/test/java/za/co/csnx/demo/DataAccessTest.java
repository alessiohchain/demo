package za.co.csnx.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import za.co.csnx.engine.common.DataAccessScanner;

/**
 * This module's main sources against the fleet's data-access rules — the rules
 * themselves live in the engine's {@link DataAccessScanner} (one copy, pinned by its
 * own test); this class owns only the folder and the allow-list
 * (platform/docs/module-performance-plan.md §3.B / §3.C).
 *
 * <ul>
 *   <li>No rows loaded and then reduced in Java: filter, limit and offset go into
 *       the query.</li>
 *   <li>JDBC only with a stated reason: JPA repositories are the default, and a class
 *       that needs JDBC says why in a {@code JDBC:} javadoc line
 *       ({@code platform/docs/dao-patterns.md} §"JDBC is the exception").</li>
 * </ul>
 */
class DataAccessTest {

    private static final List<Path> MAIN = List.of(Path.of("src/main/java"));

    @Test
    void noRowsAreLoadedAndThenReducedInJava() throws IOException {
        assertThat(DataAccessScanner.loadThenReduce(MAIN, Map.of())).isEmpty();
    }

    @Test
    void everyJdbcUseStatesItsReason() throws IOException {
        assertThat(DataAccessScanner.jdbcWithoutReason(MAIN)).isEmpty();
    }
}
