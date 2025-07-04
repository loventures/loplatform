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

import * as React from 'react';
import { lifecycle, withState } from 'recompose';
import { compose } from 'redux';

import LoadingSpinner from './index.tsx';

type FadeInLoaderProps = {
  delay?: number;
  duration?: number;
  message: string;
};
const DEFAULT_DELAY = 1000;
const DEFAULT_DURATION = 500;

export const FadeInLoader: React.ComponentType<FadeInLoaderProps> = compose(
  withState('visible', 'setVisible', false),
  lifecycle<FadeInLoaderProps, Record<string, unknown>, any>({
    componentDidMount() {
      this.timeout = window.setTimeout(() => {
        (this.props as any).setVisible(true);
      }, this.props.delay || DEFAULT_DELAY);
    },
    componentWillUnmount() {
      window.clearInterval(this.timeout);
    },
  })
)(({ visible, duration, message }: FadeInLoaderProps & { visible: boolean }) => (
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
)) as any; // HOCs are lies;
