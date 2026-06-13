package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.csnx.engine.common.BusinessException;
import za.co.csnx.demo.domain.ShipmentFlowDetail;
import za.co.csnx.demo.repository.ShipmentFlowDetailRepository;

/**
 * CSFD (detail) activity bean. Auto-assigns {@code shipmentFlowSeq} on
 * insert, validates the trader exists, and provides the
 * parent-scoped {@link #findByParent} finder with trader-name
 * hydration.
 *
 * <p>DB-level NOT NULL constraints handle per-field "required"
 * rejections — only the cross-row trader-existence check lives here.
 */
@Service
public class ShipmentFlowDetailActivityBean
        extends AbstractCrudActivityBean<ShipmentFlowDetail, ShipmentFlowDetail.Pk>
        implements ShipmentFlowDetailActivity {

    private final ShipmentFlowDetailRepository detailRepository;
    private final TraderActivity traderActivity;

    public ShipmentFlowDetailActivityBean(ShipmentFlowDetailRepository detailRepository,
                                          TraderActivity traderActivity) {
        super(detailRepository);
        this.detailRepository = detailRepository;
        this.traderActivity = traderActivity;
    }

    @Override
    protected void beforeSave(ShipmentFlowDetail e, ValidationMode mode) {
        if (mode == ValidationMode.INSERT) {
            if (e.getShipmentFlowSeq() == null) {
                int next = detailRepository
                        .findMaxShipmentFlowSeq(e.getCompanyCode(), e.getShipmentFlow())
                        .orElse(0) + 10;
                e.setShipmentFlowSeq(next);
            }
            // Screen-tran stamp on insert only (mirrors CSnx).
            e.setMaintenanceTran("CSFD");
        }
    }

    @Override
    protected void validate(ShipmentFlowDetail e, ValidationMode mode) {
        // DB NOT NULL handles required-field rejections. Only the
        // cross-row lookup belongs here. Hydrate the transient
        // {@code traderName} from the looked-up trader so cmd_create /
        // cmd_update responses ship a fully-populated row — without
        // this, the grid shows a blank Trader Name column until a
        // manual cmd_search re-runs the trader-name join.
        za.co.csnx.demo.domain.Trader trader = traderActivity
                .findOne(e.getCompanyCode(), e.getTraderType(), e.getTraderCode())
                .orElseThrow(() -> BusinessException.error(
                        "Trader '" + e.getTraderCode() + "' of type '" + e.getTraderType() + "' not found"));
        e.setTraderName(trader.getTraderName());
    }

    /** Insert override — {@code shipmentFlowId} is BIGSERIAL so the
     *  base impl's duplicate-key check would never fire meaningfully.
     *  Skip it. */
    @Override
    @Transactional
    public ShipmentFlowDetail insert(ShipmentFlowDetail entity) {
        beforeSave(entity, ValidationMode.INSERT);
        validate(entity, ValidationMode.INSERT);
        return repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentFlowDetail> findByParent(String companyCode, String shipmentFlow) {
        List<ShipmentFlowDetail> rows = detailRepository
                .findByCompanyCodeAndShipmentFlowOrderByShipmentFlowSeqAsc(companyCode, shipmentFlow);
        if (rows.isEmpty()) return rows;
        Map<String, String> names = traderActivity.namesByCompany(companyCode);
        for (ShipmentFlowDetail row : rows) {
            row.setTraderName(names.get(row.getTraderType() + "|" + row.getTraderCode()));
        }
        return rows;
    }
}
