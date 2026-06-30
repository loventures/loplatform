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

import { Omit } from '../../types/omit';
import React, { useEffect, useRef } from 'react';

type FocusableElement = HTMLElement;

export type WithFocusOnMount = {
  onRef: any;
};

export const withFocusOnMount = <P extends WithFocusOnMount>(
  BaseComponent: React.ComponentType<P>
) => {
  const WithFocusOnMount = (props: Omit<P, keyof WithFocusOnMount>) => {
    const ref = useRef<undefined | FocusableElement>(undefined);

    const onRef = (element: HTMLElement) => (ref.current = element);

    useEffect(() => {
      if (ref.current) {
        ref.current.focus();
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
      <BaseComponent
        {...(props as P)}
        onRef={onRef}
      />
    );
  };
  return WithFocusOnMount;
};

export default withFocusOnMount;
