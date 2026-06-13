package za.co.csnx.demo.service.activity;
import za.co.csnx.engine.activity.*;

import za.co.csnx.demo.business.activity.ReportTextActivity;

import org.springframework.stereotype.Service;
import za.co.csnx.demo.domain.ReportText;

/** RPTM workflow service — wire-shape adapter. */
@Service
public class ReportTextActivityService extends AbstractCrudActivityService<ReportText, ReportText.Pk> {

    public ReportTextActivityService(ReportTextActivity reportTextActivity) {
        super(reportTextActivity, ReportText.class);
    }

    @Override
    public String workflow() {
        return "reportText.maintenance";
    }

    @Override
    protected void applyCompanyScope(ReportText entity, String companyCode) {
        entity.setCompanyCode(companyCode);
    }
}
