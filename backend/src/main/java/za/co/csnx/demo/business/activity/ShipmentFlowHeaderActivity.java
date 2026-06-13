package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import java.util.List;
import za.co.csnx.demo.domain.ShipmentFlowDetail;
import za.co.csnx.demo.domain.ShipmentFlowHeader;

/** Interface for the COSF (master) activity bean.
 *
 * <p>Adds two single-row / child-list lookups used by the
 * {@code cmd_details} custom verb on
 * {@link CorporateShipmentFlowActivityService}. */
public interface ShipmentFlowHeaderActivity extends CrudActivity<ShipmentFlowHeader, ShipmentFlowHeader.Pk> {

    ShipmentFlowHeader findOneOrThrow(ShipmentFlowHeader.Pk id);

    List<ShipmentFlowDetail> findChildren(ShipmentFlowHeader.Pk id);
}
