import { Toaster } from 'sonner';
import { useTheme } from '@/app/theme/ThemeProvider';

/**
 * Binds sonner's colour scheme to the active theme so toasts flip with
 * the rest of the app instead of staying stuck in light mode. Visual
 * tokens (surface, border, radius, typography) come from CSS overrides
 * in {@code index.css} keyed off {@code [data-sonner-toaster]}.
 */
export function ThemedToaster() {
  const { effectiveMode } = useTheme();
  return (
    <Toaster
      theme={effectiveMode}
      position="top-center"
      richColors
      closeButton
      offset={16}
      gap={10}
      toastOptions={{
        classNames: {
          toast: 'csnx-toast',
        },
      }}
    />
  );
}
