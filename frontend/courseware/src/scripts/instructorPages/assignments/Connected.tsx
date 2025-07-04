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

import { CourseState, courseReduxStore } from '../../loRedux';
import * as React from 'react';
import { Unsubscribe, Dispatch } from 'redux';

export type ConnectedSelector<T> = (state: CourseState) => T;

type ConnectedProps<T> = {
  /**
   * A function that 'picks' things out of CourseState and returns them
   */
  selector: ConnectedSelector<T>;
  /**
   * A function that takes the return value of selector, and returns the view.
   */
  children: (t: [T, Dispatch]) => React.ReactNode;
};

/**
 * A render prop that maps redux state to parameters of a render function.
 *
 * Use this to construct React views that depend on values in the redux store.
 *
 * The subscription is only used to trigger a rerender, not to compute state.
 */
export class Connected<T> extends React.Component<ConnectedProps<T>> {
  unsubscribe?: Unsubscribe;
  constructor(props: ConnectedProps<T>) {
    super(props);
  }

  componentWillMount() {
    this.unsubscribe = courseReduxStore.subscribe(() => this.setState({}));
  }

  componentWillUnmount() {
    if (this.unsubscribe) {
      this.unsubscribe();
    }
  }

  render() {
    return this.props.children([
      this.props.selector(courseReduxStore.getState()),
      courseReduxStore.dispatch,
    ]);
  }
}
