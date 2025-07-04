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

import React from 'react';

type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

interface InferableHOC<ProvidedProps> {
  <B extends ProvidedProps>(
    c: React.ComponentType<B>
  ): React.ComponentType<Omit<B, keyof ProvidedProps>>;
}

type WithPromiseContext<A> =
  | {
      loading: true;
    }
  | {
      loading: false;
      reloading: boolean;
      reload: () => void;
      value: A;
    };

declare function withPromise<A>(opts: {
  get: (a: any) => Promise<A>;
}): InferableHOC<WithPromiseContext<A>>;
