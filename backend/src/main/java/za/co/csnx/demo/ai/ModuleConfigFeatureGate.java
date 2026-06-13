package za.co.csnx.demo.ai;

import org.springframework.stereotype.Component;
import za.co.csnx.engine.ai.spi.FeatureGate;
import za.co.csnx.engine.registry.PlatformMetadataSource;

/**
 * Demo's {@link FeatureGate}: maps each engine-ai feature to this module's live
 * {@code module_config} row (cached in {@link PlatformMetadataSource}, kept
 * fresh by the registrar). Reads the SAME config the bundle's feature flags come
 * from, so the AssistantBar UI-hide and the {@code /api/assistant} 403 stay in
 * sync. Overrides the engine's all-enabled default via component scan.
 */
@Component
public class ModuleConfigFeatureGate implements FeatureGate {

    private final PlatformMetadataSource metadataSource;

    public ModuleConfigFeatureGate(PlatformMetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    @Override
    public boolean isEnabled(Feature feature) {
        var c = metadataSource.moduleConfig();
        return switch (feature) {
            case SMART_NAVIGATION -> c.smartNavigationEnabled();
            case DASHBOARD -> c.dashboardEnabled();
            case SMART_REPORTS -> c.smartReportsEnabled();
            case SMART_CAPTURE -> c.smartCaptureEnabled();
            case SCHEDULING -> c.schedulingEnabled();
        };
    }
}
