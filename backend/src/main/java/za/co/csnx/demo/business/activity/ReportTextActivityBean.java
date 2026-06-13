package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import org.springframework.stereotype.Service;
import za.co.csnx.engine.common.BusinessException;
import za.co.csnx.demo.domain.ReportText;
import za.co.csnx.demo.repository.ReportTextRepository;

/** RPTM activity bean. */
@Service
public class ReportTextActivityBean
        extends AbstractCrudActivityBean<ReportText, ReportText.Pk>
        implements ReportTextActivity {

    public ReportTextActivityBean(ReportTextRepository repository) {
        super(repository);
    }

    @Override
    protected BusinessException onDuplicate(ReportText.Pk id) {
        return BusinessException.error("That report / language / line # already exists");
    }
}
