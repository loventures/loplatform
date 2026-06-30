/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import { courseReduxStore } from '../../loRedux';
import { TranslationProvider } from '../../i18n/translationContext';
import { QueryClientProvider, queryClient } from '../../resources/queryClient';
import React from 'react';
import { createRoot } from 'react-dom/client';
import { Provider as ReduxProvider } from 'react-redux';
import { Modal } from 'reactstrap';

/**
 * The imperative React modal host — the replacement for `$uibModal.open`.
 *
 * `$uibModal.open({ component })` instantiates an Angular component bound to `&`
 * `close`/`dismiss` callbacks, which `react2angular` cannot express. This host
 * is the React analogue: `openReactModal(renderBody)` mounts its own React root
 * (with the shared Redux store, query client, and translation providers, so it
 * works whether opened from React or from Angular), renders `renderBody` inside
 * a reactstrap `<Modal>`, and returns a promise that **resolves on `close(value)`
 * and rejects on `dismiss(reason)`** — exactly uib-modal's `.result` contract.
 *
 * DOM contract: reactstrap renders `.modal` + `.modal-header/.modal-body/.modal-footer`,
 * which the Selenide page objects select on — keep using `<ModalHeader>/<ModalBody>/
 * <ModalFooter>` in modal bodies so those selectors and footer button order hold.
 */

export interface ModalControls<T = any> {
  /** Resolve the modal promise (the uib `close`). */
  close: (value?: T) => void;
  /** Reject the modal promise (the uib `dismiss`); also fired on backdrop/ESC. */
  dismiss: (reason?: any) => void;
}

export interface OpenModalOptions {
  /** reactstrap backdrop: `true` (default), `false`, or `'static'` (no dismiss-on-click). */
  backdrop?: boolean | 'static';
  size?: 'sm' | 'lg' | 'xl';
  className?: string;
  /** Whether ESC / backdrop click dismisses (default true; `false` for forced choices). */
  keyboard?: boolean;
}

/**
 * The modal promise, plus an imperative `dismiss` so the opener can close the
 * modal programmatically (e.g. an Angular component's `$onDestroy`) — the
 * analogue of uib's `modalInstance.dismiss()`.
 */
export type ModalHandle<T = any> = Promise<T> & { dismiss: (reason?: any) => void };

const Providers: React.FC<React.PropsWithChildren> = ({ children }) => (
  <ReduxProvider store={courseReduxStore}>
    <QueryClientProvider client={queryClient}>
      <TranslationProvider>{children}</TranslationProvider>
    </QueryClientProvider>
  </ReduxProvider>
);

export function openReactModal<T = any>(
  renderBody: (controls: ModalControls<T>) => React.ReactNode,
  opts: OpenModalOptions = {}
): ModalHandle<T> {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);

  // Captured from inside the executor (which runs synchronously) so the handle
  // can expose an imperative dismiss.
  let dismissFn: (reason?: any) => void = () => {};

  const promise = new Promise<T>((resolve, reject) => {
    let settled = false;

    // Re-render closed so reactstrap can animate the fade-out, then tear down.
    const teardown = () => {
      renderModal(false);
      setTimeout(() => {
        root.unmount();
        container.remove();
      }, 300);
    };

    const close = (value?: T) => {
      if (settled) return;
      settled = true;
      resolve(value as T);
      teardown();
    };

    const dismiss = (reason?: any) => {
      if (settled) return;
      settled = true;
      reject(reason);
      teardown();
    };

    function renderModal(isOpen: boolean) {
      root.render(
        <Providers>
          <Modal
            isOpen={isOpen}
            toggle={() => dismiss('backdrop click')}
            backdrop={opts.backdrop ?? true}
            keyboard={opts.keyboard ?? true}
            size={opts.size}
            className={opts.className}
          >
            {renderBody({ close, dismiss })}
          </Modal>
        </Providers>
      );
    }

    dismissFn = dismiss;
    renderModal(true);
  }) as ModalHandle<T>;

  promise.dismiss = (reason?: any) => dismissFn(reason);
  return promise;
}
