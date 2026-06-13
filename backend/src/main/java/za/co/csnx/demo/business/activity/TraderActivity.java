package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import java.util.Map;
import java.util.Optional;
import za.co.csnx.demo.domain.Trader;

/** Interface for the TRDP / trader-lookup activity bean.
 *
 * <p>Adds two cross-bean helpers that COSF + CSFD use to hydrate the
 * transient {@code traderName} field on shipment-flow detail rows. */
public interface TraderActivity extends CrudActivity<Trader, Trader.Pk> {

    /** Look up one trader for existence checks (used by the CSFD
     *  detail bean's validate hook). */
    Optional<Trader> findOne(String companyCode, String traderType, String traderCode);

    /** Trader-code → trader-name map for the requested company. Used
     *  by COSF + CSFD to hydrate transient {@code traderName} on
     *  detail rows for grid display. */
    Map<String, String> namesByCompany(String companyCode);
}
