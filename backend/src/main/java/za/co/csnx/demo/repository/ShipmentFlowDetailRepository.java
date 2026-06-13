package za.co.csnx.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import za.co.csnx.engine.common.BaseRepository;
import za.co.csnx.demo.domain.ShipmentFlowDetail;

public interface ShipmentFlowDetailRepository
        extends BaseRepository<ShipmentFlowDetail, ShipmentFlowDetail.Pk> {

    List<ShipmentFlowDetail> findByCompanyCodeAndShipmentFlowOrderByShipmentFlowSeqAsc(
            String companyCode, String shipmentFlow);

    @Query("SELECT MAX(d.shipmentFlowSeq) FROM ShipmentFlowDetail d "
            + "WHERE d.companyCode = ?1 AND d.shipmentFlow = ?2")
    Optional<Integer> findMaxShipmentFlowSeq(String companyCode, String shipmentFlow);
}
