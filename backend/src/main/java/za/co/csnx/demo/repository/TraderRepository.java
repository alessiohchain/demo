package za.co.csnx.demo.repository;

import java.util.List;
import java.util.Optional;
import za.co.csnx.engine.common.BaseRepository;
import za.co.csnx.demo.domain.Trader;

public interface TraderRepository extends BaseRepository<Trader, Trader.Pk> {

    List<Trader> findByCompanyCodeOrderByTraderTypeAscTraderCodeAsc(String companyCode);

    Optional<Trader> findByCompanyCodeAndTraderTypeAndTraderCode(
            String companyCode, String traderType, String traderCode);
}
