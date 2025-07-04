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

import { pipe } from 'fp-ts/es6/function';
import { some } from 'lodash/fp';
import React, { ComponentType, ReactNode } from 'react';

export function nodeIsType<P>(c: ComponentType<P>): (n: any) => n is ComponentType<P> {
  return ((n: any) => (n && typeof n === 'object' && 'type' in n ? n.type === c : false)) as any;
}

export function countTypes(children: ReactNode, cs: ComponentType<any>[]): number {
  return React.Children.toArray(children).filter(n =>
    pipe(
      cs,
      some(c => nodeIsType(c)(n))
    )
  ).length;
}

export type EmptyProps = Record<string, never>;
