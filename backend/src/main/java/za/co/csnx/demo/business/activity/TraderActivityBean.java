package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.csnx.demo.domain.Trader;
import za.co.csnx.demo.repository.TraderRepository;

/**
 * TRDP / trader-lookup activity bean. Backs the trader picker AND
 * provides {@link #namesByCompany} for trader-name hydration — same
 * bean injected by both COSF and CSFD.
 *
 * <p>Write verbs are inherited from the base impl; the engine never
 * routes write verbs to a picker workflow, so the inherited paths sit
 * unused. No defensive throwing overrides — dead-code paths add noise
 * without protection.
 */
@Service
public class TraderActivityBean
        extends AbstractCrudActivityBean<Trader, Trader.Pk>
        implements TraderActivity {

    public TraderActivityBean(TraderRepository repository) {
        super(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Trader> findOne(String companyCode, String traderType, String traderCode) {
        return ((TraderRepository) repository)
                .findByCompanyCodeAndTraderTypeAndTraderCode(companyCode, traderType, traderCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> namesByCompany(String companyCode) {
        Map<String, String> out = new HashMap<>();
        for (Trader t : ((TraderRepository) repository)
                .findByCompanyCodeOrderByTraderTypeAscTraderCodeAsc(companyCode)) {
            out.put(t.getTraderType() + "|" + t.getTraderCode(), t.getTraderName());
        }
        return out;
    }
}
