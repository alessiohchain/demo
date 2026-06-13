package za.co.csnx.demo.service.activity;
import za.co.csnx.engine.activity.*;

import za.co.csnx.demo.business.activity.ShipmentFlowDetailActivity;
import za.co.csnx.demo.business.activity.ShipmentFlowHeaderActivity;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import za.co.csnx.demo.domain.ShipmentFlowDetail;
import za.co.csnx.demo.domain.ShipmentFlowHeader;
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessModelHolder;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

/**
 * COSF workflow service. Standard CRUD on {@link ShipmentFlowHeader}
 * plus the custom {@code cmd_details} verb that navigates the front
 * end to the CSFD workflow with master + children pre-populated.
 *
 * <p>{@code cmd_details} doesn't guard against multi-select or null
 * row data — the screen JSON's toolbar predicate enables the button
 * only with exactly one row selected, and grid rows always carry data.
 */
@Service
public class CorporateShipmentFlowActivityService
        extends AbstractCrudActivityService<ShipmentFlowHeader, ShipmentFlowHeader.Pk> {

    static final String DETAIL_WORKFLOW = "corporateShipmentFlowDetails.maintenance";
    static final String CMD_DETAILS = "cmd_details";

    private final ShipmentFlowHeaderActivity shipmentFlowHeaderActivity;
    private final ShipmentFlowDetailActivity shipmentFlowDetailActivity;

    public CorporateShipmentFlowActivityService(ShipmentFlowHeaderActivity shipmentFlowHeaderActivity,
                                                ShipmentFlowDetailActivity shipmentFlowDetailActivity) {
        super(shipmentFlowHeaderActivity, ShipmentFlowHeader.class);
        this.shipmentFlowHeaderActivity = shipmentFlowHeaderActivity;
        this.shipmentFlowDetailActivity = shipmentFlowDetailActivity;
    }

    @Override
    public String workflow() {
        return "corporateShipmentFlows";
    }

    @Override
    protected void applyCompanyScope(ShipmentFlowHeader entity, String companyCode) {
        entity.setCompanyCode(companyCode);
    }

    @Override
    public ProcessResponse cmdCustom(ProcessRequest request) {
        if (!CMD_DETAILS.equals(request.command())) {
            return super.cmdCustom(request);
        }
        String companyCode = currentCompanyCode();
        Map<String, Object> data = pickSelected(request).get(0).data();
        ShipmentFlowHeader probe = newScoped(companyCode);
        fromData(data, probe);
        ShipmentFlowHeader.Pk id = shipmentFlowHeaderActivity.idOf(probe);
        ShipmentFlowHeader header = shipmentFlowHeaderActivity.findOneOrThrow(id);
        List<ShipmentFlowDetail> children = shipmentFlowDetailActivity.findByParent(companyCode, id.getShipmentFlow());

        List<ProcessModelEnvelope> childEnvelopes = children.stream()
                .map(c -> {
                    // toDataAuto skips @Transient fields, so the post-load
                    // hydrated traderName must be overlaid onto the wire map
                    // explicitly (the detail grid's Trader Name column reads it).
                    Map<String, Object> childData = new java.util.LinkedHashMap<>(toDataAuto(c));
                    childData.put("traderName", c.getTraderName());
                    return ProcessModelEnvelope.ofData(childData);
                })
                .toList();
        return ProcessResponse.changePage(DETAIL_WORKFLOW, request.command(),
                Map.of("", ProcessModelHolder.parentChild(
                        ProcessModelEnvelope.ofData(toData(header)), childEnvelopes)));
    }
}
