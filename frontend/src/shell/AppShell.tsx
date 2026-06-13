import { useEffect, useRef, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { ChevronDown, KeyRound, LogOut, Menu, UserCircle2 } from 'lucide-react';
import { useAuth } from '@/app/auth/AuthProvider';
import { platformIssuer } from '@/app/auth/platformSso';
import { Sidebar } from '@/shell/Sidebar';
import { FastpathInput } from '@/shell/FastpathInput';
import { Breadcrumb } from '@alessiohchain/csnx-engine/breadcrumbs/Breadcrumb';
import { NetworkActivityBar } from '@/shell/NetworkActivityBar';
import { WarehouseSwitcher } from '@/shell/WarehouseSwitcher';
import { ThemePicker } from '@/app/theme/ThemePicker';
import { BusinessMessageHost } from '@alessiohchain/csnx-engine/process/businessMessages';
import { cn } from '@/lib/utils';

export function AppShell() {
  const { user, menu, logout, versionInfo } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  // Close the dropdown on outside click — no popover library yet, so a
  // small effect handles dismiss. The same pattern AppShell uses for
  // other transient affordances.
  useEffect(() => {
    if (!userMenuOpen) return;
    function onDocClick(e: MouseEvent) {
      if (!userMenuRef.current) return;
      if (!userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, [userMenuOpen]);

  return (
    <div className="h-screen flex flex-col overflow-hidden">
      <NetworkActivityBar />
      <header className="h-14 shrink-0 border-b bg-card flex items-center px-4 gap-4">
        <button
          type="button"
          aria-label="Toggle menu"
          onClick={() => setSidebarOpen((v) => !v)}
          className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-transparent text-muted-foreground transition-colors hover:border-primary hover:bg-primary/5 hover:text-primary"
        >
          <Menu className="h-4 w-4" aria-hidden />
        </button>
        <div className="flex items-baseline gap-1.5">
          <span className="font-semibold tracking-tight">CSnx</span>
          {versionInfo && (
            <span
              className="text-[10px] font-normal text-muted-foreground"
              title={versionInfo}
            >
              {versionInfo}
            </span>
          )}
        </div>

        <div className="ml-auto">
          <FastpathInput />
        </div>

        <div className="flex items-center gap-3 text-sm">
          <ThemePicker />
          <WarehouseSwitcher />
          {/* Username with a chevron — clicking opens the dropdown for
            * Change password. Sign out stays as its own visible button
            * so the action is always one click away (matches CSnx's
            * legacy header layout where Sign out was top-level). */}
          <div ref={userMenuRef} className="relative">
            <button
              type="button"
              onClick={() => setUserMenuOpen((v) => !v)}
              aria-haspopup="menu"
              aria-expanded={userMenuOpen}
              className="inline-flex items-center gap-1.5 text-muted-foreground rounded-md border border-transparent px-2 h-8 transition-colors hover:border-primary hover:bg-primary/5 hover:text-primary"
              title="User menu"
            >
              <UserCircle2 className="h-4 w-4" aria-hidden />
              {user?.username}
              <ChevronDown className="h-3.5 w-3.5" aria-hidden />
            </button>
            {userMenuOpen && (
              <div
                role="menu"
                className="absolute right-0 top-full mt-1 w-48 rounded-md border bg-popover shadow-md text-sm z-20 p-1"
              >
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setUserMenuOpen(false);
                    // Passwords live on the central IdP now — its styled
                    // change-password page handles the flow; company and
                    // username pre-fill via query params.
                    window.location.assign(
                      `${platformIssuer()}/change-password?company=${encodeURIComponent(user?.companyCode ?? '')}&user=${encodeURIComponent(user?.username ?? '')}`,
                    );
                  }}
                  className="flex w-full items-center gap-2 rounded-sm px-3 py-1.5 text-left transition-colors hover:bg-primary/10 hover:text-primary"
                >
                  <KeyRound className="h-3.5 w-3.5 text-muted-foreground" aria-hidden />
                  Change password
                </button>
              </div>
            )}
          </div>
          <button
            type="button"
            onClick={() => logout()}
            className="inline-flex items-center gap-1.5 rounded-md border bg-background px-3 h-8 text-sm transition-colors hover:border-primary hover:bg-primary/5 hover:text-primary"
          >
            <LogOut className="h-3.5 w-3.5" aria-hidden />
            Sign out
          </button>
        </div>
      </header>

      <div className="flex-1 flex overflow-hidden">
        <aside
          className={cn(
            'border-r bg-card transition-[width] duration-150 ease-out overflow-y-auto',
            sidebarOpen ? 'w-64' : 'w-0',
          )}
        >
          <Sidebar items={menu} />
        </aside>
        <main className="flex-1 overflow-auto bg-muted/40">
          <Breadcrumb />
          <div className="p-6">
            <Outlet />
          </div>
        </main>
      </div>
      {/* Global host for the multi-message "Please review" dialog.
        * Mounted above the route outlet so any screen / route's
        * server response can land here regardless of which workflow
        * is active. Single messages go to toast and never enter this
        * host; only 2+ messages trigger the modal listener. */}
      <BusinessMessageHost />
    </div>
  );
}
