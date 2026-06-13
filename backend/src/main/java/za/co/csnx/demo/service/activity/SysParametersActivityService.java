package za.co.csnx.demo.service.activity;
import za.co.csnx.engine.activity.*;

import za.co.csnx.demo.business.activity.SysParametersActivity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.csnx.engine.common.BusinessException;
import za.co.csnx.demo.domain.SysParameters;
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

/**
 * WSPM workflow service — singleton-edit. Sibling of
 * {@link AbstractCrudActivityService} on the engine hierarchy; mirrors
 * CSnx's {@code WaddActivityService_WSPM extends BaseActivityService}.
 *
 * <p>Screen-tran stamp ({@code "WSPM"}) lives on the activity's
 * {@code beforeSave} hook, not here — consistent with CSFD's pattern.
 */
@Service
public class SysParametersActivityService extends AbstractEngineActivity<SysParameters> {

    private final SysParametersActivity sysParametersActivity;

    public SysParametersActivityService(SysParametersActivity sysParametersActivity) {
        super(SysParameters.class);
        this.sysParametersActivity = sysParametersActivity;
    }

    @Override
    public String workflow() {
        return "sysParameters.maintenance";
    }

    @Override
    protected void applyCompanyScope(SysParameters entity, String companyCode) {
        entity.setCompanyCode(companyCode);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessResponse cmdSearch(ProcessRequest request) {
        String companyCode = currentCompanyCode();
        SysParameters entity = sysParametersActivity.loadOrDefault(companyCode, () -> newScoped(companyCode));
        return okForm(request.command(), ProcessModelEnvelope.ofData(toData(entity)));
    }

    @Override
    public ProcessResponse cmdUpdate(ProcessRequest request) {
        String companyCode = currentCompanyCode();
        SysParameters entity = sysParametersActivity.loadOrDefault(companyCode, () -> newScoped(companyCode));
        fromData(singleModelData(request), entity);
        applyAuditStamps(entity, currentUsername(), TRAN_UPDATE);
        return okForm(request.command(), ProcessModelEnvelope.ofData(toData(sysParametersActivity.save(entity))));
    }

    @Override
    public ProcessResponse cmdCreate(ProcessRequest request) {
        throw BusinessException.warning("System parameters: edit only — use cmd_update to save changes");
    }

    @Override
    public ProcessResponse cmdDelete(ProcessRequest request) {
        throw BusinessException.warning("System parameters: edit only — not deletable");
    }

    @Override
    public ProcessResponse cmdCopy(ProcessRequest request) {
        throw BusinessException.warning("System parameters: edit only");
    }
}
