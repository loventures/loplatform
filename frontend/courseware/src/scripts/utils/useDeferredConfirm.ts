/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

import { useState } from 'react';

import useDeferred, { DeferredReCreate, DeferredReject, DeferredResolve } from './useDeferred';

type DeferredConfirmTuple<T> = [
  () => ReturnType<DeferredReCreate<T>>,
  boolean,
  DeferredResolve<T>,
  DeferredReject,
];

export const useDeferredConfirm = <T = void>(): DeferredConfirmTuple<T> => {
  const [isOpen, setIsOpen] = useState(false);
  const [defer, resolve, reject] = useDeferred<T>();

  const deferConfirm = () => {
    return defer(() => {
      setIsOpen(true);
    });
  };

  const confirm = (value: Parameters<DeferredResolve<T>>[0]) => {
    setIsOpen(false);
    resolve(value);
  };

  const cancel = () => {
    setIsOpen(false);
    reject();
  };

  return [deferConfirm, isOpen, confirm, cancel];
};
