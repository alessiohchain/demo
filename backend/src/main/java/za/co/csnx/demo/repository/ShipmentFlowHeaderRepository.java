package za.co.csnx.demo.repository;

import java.util.List;
import za.co.csnx.engine.common.BaseRepository;
import za.co.csnx.demo.domain.ShipmentFlowHeader;

public interface ShipmentFlowHeaderRepository
        extends BaseRepository<ShipmentFlowHeader, ShipmentFlowHeader.Pk> {

    List<ShipmentFlowHeader> findByCompanyCodeOrderByShipmentFlowAsc(String companyCode);
}
