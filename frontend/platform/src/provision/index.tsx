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

import _ from 'lodash';

import React, { lazy, Suspense } from 'react';

import WaitDotGif from '../components/WaitDotGif';

const LazyApp = lazy(() => import('./ProvisionPage'));

const ProvisionDomain: React.FC = () => (
  <Suspense
    fallback={
      <WaitDotGif
        color="secondary"
        size={64}
      />
    }
  >
    <LazyApp />
  </Suspense>
);

export const extendObject = <T extends Record<string, any>>(
  object: T,
  property: string | undefined,
  value: unknown
): T => {
  const newObject = { ...object };
  if (property) (newObject as Record<string, any>)[property] = _.defaultTo(value, '');
  return newObject;
};

export default ProvisionDomain;
