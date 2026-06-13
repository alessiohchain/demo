package za.co.csnx.demo.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.csnx.engine.common.BusinessException;
import za.co.csnx.engine.security.GrantEnforcer;
import za.co.csnx.engine.activity.ActivityRegistry;
import za.co.csnx.engine.activity.ActivityService;
import za.co.csnx.engine.web.dto.ExceptionEnvelope;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

/**
 * POST /api/process — central dispatcher for every command the UI engine fires.
 * Resolves the workflow's {@link ActivityService} from the
 * {@link ActivityRegistry} and delegates. A command-level grant check (engine
 * §16) 403s an ungranted/read-only fastpath before dispatch; otherwise the
 * engine convention is to return 200 with any failure packed into the response
 * envelope.
 */
@RestController
@RequestMapping("/api/process")
public class ProcessController {

    private static final Logger log = LoggerFactory.getLogger(ProcessController.class);

    private final ActivityRegistry registry;
    private final za.co.csnx.engine.registry.PlatformMetadataSource metadataSource;
    private final GrantEnforcer grantEnforcer;

    public ProcessController(ActivityRegistry registry,
                             za.co.csnx.engine.registry.PlatformMetadataSource metadataSource,
                             GrantEnforcer grantEnforcer) {
        this.registry = registry;
        this.metadataSource = metadataSource;
        this.grantEnforcer = grantEnforcer;
    }

    @PostMapping
    public ResponseEntity<ProcessResponse> process(@RequestBody ProcessRequest request) {
        String workflow = request.workflow();
        String command = request.command();
        log.debug("/api/process workflow={} command={}", workflow, command);
        metadataSource.fastpathOf(workflow)
                .filter(fastpath -> fastpath != null && !fastpath.isBlank())
                .ifPresent(fastpath -> grantEnforcer.requireCommand(fastpath, command));
        try {
            ActivityService activity = registry.get(workflow);
            ProcessResponse response = activity.process(request);
            return ResponseEntity.ok(response);
        } catch (BusinessException ex) {
            log.info("/api/process business failure workflow={} command={} messages={}",
                    workflow, command, ex.getMessages().size());
            return ResponseEntity.ok(ProcessResponse.failure(workflow, command,
                    ExceptionEnvelope.fromBusiness(ex)));
        } catch (ActivityRegistry.UnknownWorkflowException ex) {
            log.warn("/api/process unknown workflow={}", workflow);
            return ResponseEntity.ok(ProcessResponse.failure(workflow, command,
                    ExceptionEnvelope.fromThrowable(ex)));
        } catch (UnsupportedOperationException ex) {
            log.debug("/api/process silent-noop workflow={} command={}", workflow, command);
            return ResponseEntity.ok(ProcessResponse.ok(workflow, command, java.util.Map.of()));
        } catch (RuntimeException ex) {
            log.error("/api/process internal failure workflow={} command={}", workflow, command, ex);
            return ResponseEntity.ok(ProcessResponse.failure(workflow, command,
                    ExceptionEnvelope.fromThrowable(ex)));
        }
    }
}
