package za.co.csnx.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import za.co.csnx.engine.common.BaseRepository;
import za.co.csnx.demo.domain.Company;

public interface CompanyRepository extends BaseRepository<Company, String> {

    @Query("SELECT c FROM Company c WHERE c.active = true ORDER BY c.companyCode")
    List<Company> findAllActive();
}
