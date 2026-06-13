package za.co.csnx.demo.service.activity;
import za.co.csnx.engine.activity.*;

import za.co.csnx.demo.business.activity.TraderActivity;

import org.springframework.stereotype.Service;
import za.co.csnx.demo.domain.Trader;

/**
 * TRDP workflow service — trader picker. Inherits cmd_search via the
 * base class's probe-based default; the engine never routes write
 * verbs to a picker workflow so the inherited write paths are
 * dead-code-safe.
 */
@Service
public class TraderPromptActivityService extends AbstractCrudActivityService<Trader, Trader.Pk> {

    public TraderPromptActivityService(TraderActivity traderActivity) {
        super(traderActivity, Trader.class);
    }

    @Override
    public String workflow() {
        return "trader.prompt";
    }

    @Override
    protected void applyCompanyScope(Trader entity, String companyCode) {
        entity.setCompanyCode(companyCode);
    }
}
