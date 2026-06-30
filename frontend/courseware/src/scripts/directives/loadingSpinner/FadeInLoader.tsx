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

import * as React from 'react';
import { useEffect, useState } from 'react';

import LoadingSpinner from './index.tsx';

type FadeInLoaderProps = {
  delay?: number;
  duration?: number;
  message: string;
};
const DEFAULT_DELAY = 1000;
const DEFAULT_DURATION = 500;

const FadeInLoaderInner = ({
  visible,
  duration,
  message,
}: FadeInLoaderProps & { visible: boolean }) => (
  <React.Fragment>
    <LoadingSpinner
      message={message}
      style={{
        opacity: visible ? 1 : 0,
        transitionTimingFunction: 'ease-in',
        transitionProperty: 'opacity',
        transitionDuration: `${duration || DEFAULT_DURATION}ms`,
      }}
    />
  </React.Fragment>
);

export const FadeInLoader: React.ComponentType<FadeInLoaderProps> = (
  props: FadeInLoaderProps
) => {
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setVisible(true);
    }, props.delay || DEFAULT_DELAY);
    return () => window.clearInterval(timeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <FadeInLoaderInner
      {...props}
      visible={visible}
    />
  );
};
