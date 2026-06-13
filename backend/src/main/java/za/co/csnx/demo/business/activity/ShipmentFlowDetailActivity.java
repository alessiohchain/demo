package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import java.util.List;
import za.co.csnx.demo.domain.ShipmentFlowDetail;

/** Interface for the CSFD (detail) activity bean.
 *
 * <p>Adds the parent-scoped finder used by the master-detail
 * {@code refreshedChildren} re-query path after delete. Trader-name
 * hydration is included so the grid shows human-readable names
 * alongside codes. */
public interface ShipmentFlowDetailActivity extends CrudActivity<ShipmentFlowDetail, ShipmentFlowDetail.Pk> {

    /** Fetch detail rows for one parent header, with each row's
     *  transient {@code traderName} hydrated via
     *  {@link TraderActivity#namesByCompany}. */
    List<ShipmentFlowDetail> findByParent(String companyCode, String shipmentFlow);
}
