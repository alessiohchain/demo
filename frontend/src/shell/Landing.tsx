import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/app/auth/AuthProvider';
import { clearBreadcrumbs } from '@alessiohchain/csnx-engine/breadcrumbs/breadcrumbStore';
import { resetWorkflowHistory } from '@alessiohchain/csnx-engine/process/workflowTracker';
import { startFreshScreenFlow } from '@alessiohchain/csnx-engine/screen/screenFlow';

export function Landing() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  // Hitting the landing page from anywhere — Close/Cancel cascade, Sign
  // out, fastpath header logo, browser Home button — is a return to the
  // root. Wipe the trail, the workflow tracker, and every transient
  // store carrying per-screen state forward, so the next fastpath /
  // menu click starts truly fresh (no stale form values, grid rows,
  // popup snapshots, or cached server responses surfacing on the
  // destination screen).
  useEffect(() => {
    clearBreadcrumbs();
    resetWorkflowHistory();
    startFreshScreenFlow(queryClient);
  }, [queryClient]);
  return (
    <div className="space-y-2">
      <h1 className="text-2xl font-semibold">Welcome, {user?.displayName ?? user?.username}</h1>
      <p className="text-muted-foreground">
        Pick a screen from the menu, or type a four-letter fastpath in the
        header.
      </p>
    </div>
  );
}
