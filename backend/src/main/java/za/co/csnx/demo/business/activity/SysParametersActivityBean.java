package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import org.springframework.stereotype.Service;
import za.co.csnx.engine.common.BusinessException;
import za.co.csnx.demo.domain.SysParameters;
import za.co.csnx.demo.repository.SysParametersRepository;

/**
 * WSPM activity bean — singleton-edit (one row per company).
 *
 * <p>Single-field range validation lives in the screen metadata's
 * {@code min}/{@code max} declarations; this bean enforces only the
 * cross-field rules that can't be expressed there.
 *
 * <p>Stamps the {@code "WSPM"} screen-tran in {@link #beforeSave}.
 */
@Service
public class SysParametersActivityBean
        extends AbstractCrudActivityBean<SysParameters, String>
        implements SysParametersActivity {

    public SysParametersActivityBean(SysParametersRepository repository) {
        super(repository);
    }

    @Override
    protected void beforeSave(SysParameters e, ValidationMode mode) {
        e.setMaintenanceTran("WSPM");
    }

    @Override
    protected void validate(SysParameters e, ValidationMode mode) {
        if (e.getVerificationMaxLength() != null && e.getVerificationMinLength() != null
                && e.getVerificationMaxLength() < e.getVerificationMinLength()) {
            throw BusinessException.error("Verification max length must be ≥ min length");
        }
        if ("Y".equals(e.getDNoteGenerateNum())
                && (e.getDNoteNumberBasis() == null || e.getDNoteNumberBasis().isBlank())) {
            throw BusinessException.error("Generate D-Note Number requires D-Note Number Basis to be set");
        }
    }
}
