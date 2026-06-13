import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { errorBus, type MultiError } from '@/app/error/errorBus';

/**
 * Mounted once at the root. Listens to errorBus and shows a dialog with
 * the list of errors when a MultipleException-shaped response arrives.
 *
 * <p>Portaled to body and pinned above {@link Modal}'s z-50 so server-side
 * validation errors raised while a popup is open (e.g. ITBC's Add popup
 * → cmd_create → MultipleException) actually paint on top of the popup
 * instead of behind it.
 */
export function MultiErrorDialog() {
  const [active, setActive] = useState<MultiError | null>(null);

  useEffect(() => errorBus.subscribe(setActive), []);

  useEffect(() => {
    if (!active) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setActive(null);
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [active]);

  if (!active) return null;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="multi-error-title"
      className="fixed inset-0 z-60 flex items-center justify-center bg-black/40"
    >
      <div
        className="flex max-w-md flex-col overflow-hidden rounded-lg bg-background shadow-lg"
      >
        <header className="flex items-center justify-between border-b border-primary bg-primary/5 px-6 py-2 text-primary">
          <h2 id="multi-error-title" className="text-sm font-semibold">
            {active.title}
          </h2>
          <button
            type="button"
            onClick={() => setActive(null)}
            aria-label="Close"
            className="text-primary/70 hover:text-primary"
          >
            ✕
          </button>
        </header>
        <div className="px-6 py-4">
          <ul className="space-y-2 text-sm text-destructive">
            {active.errors.map((err, idx) => (
              <li key={idx}>
                {err.field ? <strong>{err.field}: </strong> : null}
                {stripHtmlTags(err.message)}
              </li>
            ))}
          </ul>
          <div className="mt-6 flex justify-end">
            <button
              type="button"
              className="inline-flex h-9 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
              onClick={() => setActive(null)}
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body,
  );
}

/**
 * Server-side {@code MultipleException} messages occasionally arrive
 * pre-wrapped in HTML for the legacy SmartGWT dialog (e.g.
 * {@code <li>ARVE000001: Could not find PR number.</li>} — see
 * {@code findPR} activities). React renders the surrounding {@code <li>}
 * itself, so strip any inline tags from the message text rather than
 * dumping them as literal angle-brackets to the user. We don't render
 * server HTML as innerHTML — that would be an XSS gateway. Tag stripping
 * is the predictable middle ground.
 */
function stripHtmlTags(input: string | undefined): string {
  if (!input) return '';
  // Remove tag pairs and standalone tags; collapse whitespace introduced
  // by stripped wrappers so the message reads cleanly.
  return input
    .replace(/<\/?[a-zA-Z][a-zA-Z0-9]*\b[^>]*>/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}
