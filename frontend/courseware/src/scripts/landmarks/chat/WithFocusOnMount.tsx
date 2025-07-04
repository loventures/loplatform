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

import { Omit } from '../../types/omit';
import { compose, lifecycle, withHandlers } from 'recompose';

type FocusableElement = HTMLElement;

const addHandler = withHandlers(() => {
  let ref: undefined | FocusableElement;

  return {
    onRef: () => (element: HTMLElement) => (ref = element),
    focusRef: () => () => {
      if (ref) {
        ref.focus();
      }
    },
  };
});

type LifecycleProps = {
  focusRef: any;
};

const addLifeCycle = lifecycle<LifecycleProps, Record<string, unknown>>({
  componentDidMount() {
    this.props.focusRef();
  },
});

export type WithFocusOnMount = {
  onRef: any;
};

// const withFocusOnMount = compose<any,any>(addHandler, addLifeCycle);

export const withFocusOnMount = <P extends WithFocusOnMount>(
  BaseComponent: React.ComponentType<P>
) => {
  return compose<P, Omit<P, keyof WithFocusOnMount>>(addHandler, addLifeCycle)(BaseComponent);
};

export default withFocusOnMount;
