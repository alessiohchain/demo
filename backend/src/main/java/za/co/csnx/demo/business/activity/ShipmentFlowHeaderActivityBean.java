package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.csnx.engine.common.BusinessException;
import za.co.csnx.demo.domain.ShipmentFlowDetail;
import za.co.csnx.demo.domain.ShipmentFlowHeader;
import za.co.csnx.demo.repository.ShipmentFlowDetailRepository;
import za.co.csnx.demo.repository.ShipmentFlowHeaderRepository;

/**
 * COSF (master) activity bean. Owns the cascade-delete-children logic
 * because the DB has {@code ON DELETE RESTRICT}, plus the two
 * single-row / child-list lookups the {@code cmd_details} verb needs.
 */
@Service
public class ShipmentFlowHeaderActivityBean
        extends AbstractCrudActivityBean<ShipmentFlowHeader, ShipmentFlowHeader.Pk>
        implements ShipmentFlowHeaderActivity {

    private final ShipmentFlowHeaderRepository headerRepository;
    private final ShipmentFlowDetailRepository detailRepository;

    public ShipmentFlowHeaderActivityBean(ShipmentFlowHeaderRepository headerRepository,
                                          ShipmentFlowDetailRepository detailRepository) {
        super(headerRepository);
        this.headerRepository = headerRepository;
        this.detailRepository = detailRepository;
    }

    @Override
    @Transactional
    public void deleteById(ShipmentFlowHeader.Pk id) {
        detailRepository
                .findByCompanyCodeAndShipmentFlowOrderByShipmentFlowSeqAsc(
                        id.getCompanyCode(), id.getShipmentFlow())
                .forEach(detailRepository::delete);
        headerRepository.findById(id).ifPresent(headerRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentFlowHeader findOneOrThrow(ShipmentFlowHeader.Pk id) {
        return headerRepository.findById(id)
                .orElseThrow(() -> BusinessException.error("Shipment flow not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentFlowDetail> findChildren(ShipmentFlowHeader.Pk id) {
        return detailRepository.findByCompanyCodeAndShipmentFlowOrderByShipmentFlowSeqAsc(
                id.getCompanyCode(), id.getShipmentFlow());
    }
}
