package za.co.csnx.demo.business.activity;
import za.co.csnx.engine.activity.*;

import za.co.csnx.demo.domain.SysParameters;

/** Interface for the WSPM activity bean — singleton-edit. The base
 *  {@link CrudActivity#loadOrDefault} + {@link CrudActivity#save}
 *  cover the WSPM cmd_search / cmd_update verbs; no extras. */
public interface SysParametersActivity extends CrudActivity<SysParameters, String> {
}
