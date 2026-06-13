package za.co.csnx.demo.repository;

import za.co.csnx.engine.common.BaseRepository;
import za.co.csnx.demo.domain.SysParameters;

/**
 * Repository for the WSPM (System Parameters) entity. One row per
 * company, keyed by {@code companyCode}.
 */
public interface SysParametersRepository extends BaseRepository<SysParameters, String> {
}
