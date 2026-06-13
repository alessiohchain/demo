import { useEffect, useState } from 'react';
import { getInflightCount, subscribeInflight } from '@/app/api/client';

/**
 * CSNX-13939 — global "something is loading" indicator. A thin bar pinned
 * to the very top of the viewport that animates whenever ANY axios request
 * is in flight. Counts at the axios-interceptor level so it captures every
 * outbound HTTP call uniformly: TanStack-driven queries / mutations,
 * EntityDialog submits, master-detail saves, and the auto-refresh poller's
 * raw {@code api.post} calls.
 */
export function NetworkActivityBar() {
  const [count, setCount] = useState(getInflightCount());
  useEffect(() => subscribeInflight(setCount), []);
  const active = count > 0;

  // Linger briefly after the last request completes so blink-and-you-miss-it
  // calls still register visually. Clears immediately when a new call starts.
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    if (active) {
      setVisible(true);
      return;
    }
    const id = window.setTimeout(() => setVisible(false), 250);
    return () => window.clearTimeout(id);
  }, [active]);

  if (!visible) return null;

  return (
    <div
      role="progressbar"
      aria-label="Loading"
      aria-busy="true"
      className="fixed left-0 right-0 top-0 z-70 h-0.5 overflow-hidden bg-primary/20 pointer-events-none"
    >
      <div className="h-full w-1/3 animate-csnx-loader bg-primary" />
    </div>
  );
}
