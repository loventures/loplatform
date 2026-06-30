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

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { useTranslation } from '../../i18n/translationContext.tsx';
import { dismissToastThunkActionCreator } from './toastActionCreators.ts';
import { selectToastState } from './selector.js';

interface ToastModel {
  toastId: string;
  msg: string;
  cls?: string;
  dismissOnTimeout?: number | string;
}

const Toast: React.FC<{ toast: ToastModel; index: number; onClose: (id: string) => void }> = ({
  toast,
  index,
  onClose,
}) => {
  const translate = useTranslation();

  // Replaces uib-alert's `dismiss-on-timeout`.
  useEffect(() => {
    if (!toast.dismissOnTimeout) return;
    const t = setTimeout(() => onClose(toast.toastId), Number(toast.dismissOnTimeout));
    return () => clearTimeout(t);
  }, [toast.toastId, toast.dismissOnTimeout, onClose]);

  return (
    <div
      role="alert"
      className={`alert toast toast-top-left ${toast.cls ?? ''}`}
      style={{ top: `${2.5 + index * 4}rem` }}
    >
      {translate(toast.msg)}
      <button
        type="button"
        className="btn-close"
        aria-label={translate('CLOSE')}
        onClick={() => onClose(toast.toastId)}
      />
    </div>
  );
};

/**
 * React port of the `toastContainer` component (the top-left toast stack). Reads the
 * toast Redux slice with useSelector and dismisses via the existing `ToastActions`
 * thunk; uib-alert → a plain alert div with its own dismiss-on-timeout effect.
 * Previously an Angular component exposed to React via angular2react — now native
 * React (its only renderer is ERAppContainer); the Angular module is kept (minus the
 * component) so the Angular app still pulls in the toast slice via `toast.name`.
 */
export const ToastContainer: React.FC = () => {
  const { toasts } = useSelector(selectToastState) as unknown as {
    toasts: { listState: ToastModel[] };
  };
  const dispatch = useDispatch();

  const closeToast = (toastId: string) => {
    dispatch(dismissToastThunkActionCreator(toastId));
  };

  return (
    <div>
      {(toasts?.listState ?? []).map((toast, index) => (
        <Toast
          key={toast.toastId}
          toast={toast}
          index={index}
          onClose={closeToast}
        />
      ))}
    </div>
  );
};

