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

import React, { useEffect, useState } from 'react';

type LazyRenderProps = {
  doRender: boolean;
} & React.PropsWithChildren;

/**
 * Delay rendering of children till doRender param is set to true.
 * Useful for when you want to delay an api call or something
 * computationally expensive till you actually need to render it.
 *
 * @param doRender when to trigger render
 */
export const LazyRender: React.FC<LazyRenderProps> = ({ doRender, children }) => {
  const [rendered, setRendered] = useState(false);
  useEffect(() => {
    if (doRender && !rendered) {
      setRendered(doRender);
    }
  });

  return <>{rendered && children}</>;
};
