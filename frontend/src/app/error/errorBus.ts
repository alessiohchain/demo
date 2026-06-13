import type { ProblemError } from '@/app/api/client';

export interface MultiError {
  title: string;
  errors: ProblemError[];
}

type Listener = (e: MultiError) => void;

const listeners = new Set<Listener>();

export const errorBus = {
  emit(error: MultiError) {
    listeners.forEach((l) => l(error));
  },
  subscribe(listener: Listener) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
};
