package za.co.csnx.demo.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import za.co.csnx.engine.ai.spi.QueryLogRecord;
import za.co.csnx.engine.ai.spi.QueryLogStore;
import za.co.csnx.demo.domain.AssistantQueryLog;
import za.co.csnx.demo.repository.AssistantQueryLogRepository;

/**
 * Demo's {@link QueryLogStore}: persists the assistant query log into demo's own
 * {@code demo.assistant_query_log} table (the per-module "training" /
 * learning-loop surface). The engine's {@code QueryLogService} owns the
 * own-transaction + best-effort wrapping; this store only maps record↔entity
 * and enforces the per-user ownership check on feedback.
 *
 * <p><b>Writing is OFF by default</b> ({@code csnx.engine.ai.query-log.enabled}
 * unset/false) — nothing reads the log yet, so we don't accumulate rows. When
 * the flag is off this bean isn't registered and the engine's
 * {@code NoOpQueryLogStore} default takes over (assistant unaffected). Set the
 * flag {@code true} to resume writing once a training/feedback loop consumes it;
 * the table, entity and this code stay in place either way.
 */
@Component
@ConditionalOnProperty(prefix = "csnx.engine.ai.query-log", name = "enabled", havingValue = "true")
public class DemoQueryLogStore implements QueryLogStore {

    private final AssistantQueryLogRepository repo;

    public DemoQueryLogStore(AssistantQueryLogRepository repo) {
        this.repo = repo;
    }

    @Override
    public Long save(QueryLogRecord record) {
        AssistantQueryLog entry = new AssistantQueryLog();
        entry.setPrompt(record.prompt());
        entry.setWorkflow(record.workflow());
        entry.setOutcome(record.outcome());
        entry.setResultCount(record.resultCount());
        entry.setRepaired(record.repaired());
        entry.setUsername(record.username());
        entry.setCompanyCode(record.companyCode());
        return repo.save(entry).getId();
    }

    @Override
    public void markActed(Long id, String caller, boolean acted) {
        repo.findById(id).ifPresent(entry -> {
            // Ownership check — a user cannot flip another user's row (no IDOR).
            if (caller != null && caller.equals(entry.getUsername())) {
                entry.setActed(acted);
                repo.save(entry);
            }
        });
    }
}
