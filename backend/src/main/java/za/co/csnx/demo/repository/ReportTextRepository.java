package za.co.csnx.demo.repository;

import java.util.List;
import za.co.csnx.engine.common.BaseRepository;
import za.co.csnx.demo.domain.ReportText;

public interface ReportTextRepository extends BaseRepository<ReportText, ReportText.Pk> {

    List<ReportText> findByCompanyCodeOrderByReportNameAscLanguageAscTextSequenceAsc(String companyCode);
}
