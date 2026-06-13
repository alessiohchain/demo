package za.co.csnx.demo.service.activity;
import za.co.csnx.engine.activity.*;

import za.co.csnx.demo.business.activity.ShipmentFlowDetailActivity;

import java.util.List;
import org.springframework.stereotype.Service;
import za.co.csnx.demo.domain.ShipmentFlowDetail;
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

/**
 * CSFD workflow service — master-detail child. Overrides the standard
 * verbs to inject parent context + reshape responses so the engine's
 * master-detail UI works:
 * <ul>
 *   <li>{@code findForSearch} reads {@code shipmentFlow} from
 *       {@code parentModel} and seeds the probe.</li>
 *   <li>{@code cmdCreate} / {@code cmdCopy} use {@link #asSingletonModel}
 *       so EntityDialog's append branch fires.</li>
 *   <li>{@code cmdDelete} uses {@link #refreshedChildren} to re-query
 *       and ship a parentChild envelope back.</li>
 *   <li>{@code withParentInjected} / {@code withInsertContext} patch
 *       the wire data so the engine can reconstruct the composite PK
 *       and so cmd_copy gets a fresh BIGSERIAL.</li>
 * </ul>
 */
@Service
public class CorporateShipmentFlowDetailsActivityService
        extends AbstractCrudActivityService<ShipmentFlowDetail, ShipmentFlowDetail.Pk> {

    private final ShipmentFlowDetailActivity shipmentFlowDetailActivity;

    public CorporateShipmentFlowDetailsActivityService(ShipmentFlowDetailActivity shipmentFlowDetailActivity) {
        super(shipmentFlowDetailActivity, ShipmentFlowDetail.class);
        this.shipmentFlowDetailActivity = shipmentFlowDetailActivity;
    }

    @Override
    public String workflow() {
        return "corporateShipmentFlowDetails.maintenance";
    }

    @Override
    protected void applyCompanyScope(ShipmentFlowDetail entity, String companyCode) {
        entity.setCompanyCode(companyCode);
    }

    @Override
    protected List<ShipmentFlowDetail> findForSearch(String companyCode, ProcessRequest request) {
        String shipmentFlow = (String) parentValue(request, "shipmentFlow");
        if (shipmentFlow == null) {
            shipmentFlow = asString(criteria(request).get("shipmentFlow"));
        }
        if (shipmentFlow == null || shipmentFlow.isBlank()) {
            return List.of();
        }
        return shipmentFlowDetailActivity.findByParent(companyCode, shipmentFlow);
    }

    @Override
    public ProcessResponse cmdCreate(ProcessRequest request) {
        return asSingletonModel(super.cmdCreate(withInsertContext(request)));
    }

    @Override
    public ProcessResponse cmdCopy(ProcessRequest request) {
        return asSingletonModel(super.cmdCopy(withInsertContext(request)));
    }

    @Override
    public ProcessResponse cmdUpdate(ProcessRequest request) {
        return super.cmdUpdate(withParentInjected(request));
    }

    @Override
    public ProcessResponse cmdDelete(ProcessRequest request) {
        super.cmdDelete(request);
        return refreshedChildren(request, () -> queryChildEnvelopes(request));
    }

    private ProcessRequest withParentInjected(ProcessRequest request) {
        return withMutatedData(request, data -> {
            Object parentFlow = parentValue(request, "shipmentFlow");
            if (parentFlow != null && data.get("shipmentFlow") == null) {
                data.put("shipmentFlow", parentFlow);
            }
        });
    }

    private ProcessRequest withInsertContext(ProcessRequest request) {
        return withMutatedData(request, data -> {
            Object parentFlow = parentValue(request, "shipmentFlow");
            if (parentFlow != null && data.get("shipmentFlow") == null) {
                data.put("shipmentFlow", parentFlow);
            }
            data.remove("shipmentFlowId");
        });
    }

    private List<ProcessModelEnvelope> queryChildEnvelopes(ProcessRequest request) {
        Object parentFlow = parentValue(request, "shipmentFlow");
        if (parentFlow == null) return List.of();
        String companyCode = currentCompanyCode();
        return toEnvelopes(shipmentFlowDetailActivity.findByParent(companyCode, parentFlow.toString()));
    }
}
