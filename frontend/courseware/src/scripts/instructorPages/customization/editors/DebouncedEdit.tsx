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

import { debounce } from 'lodash';
import React, { ReactNode } from 'react';

type DebouncedEditProps<A, T> = {
  state: T;
  onCommit: (t: T) => void;
  debounceWait?: number;
  mapper: (a: A) => T;
  children: (c: [T, (a: A) => void]) => ReactNode;
};

type DebouncedEditState<T> = {
  context: T;
  previous: T;
};

export class DebouncedEdit<A, T> extends React.PureComponent<
  DebouncedEditProps<A, T>,
  DebouncedEditState<T>
> {
  constructor(props: DebouncedEditProps<A, T>) {
    super(props);
    this.state = {
      context: props.state,
      previous: props.state,
    };
    this.commitState = debounce(this.commitState, this.props.debounceWait || 500);
    this.updateState = this.updateState.bind(this);
  }
  static getDerivedStateFromProps<A, T>(
    props: DebouncedEditProps<A, T>,
    state: DebouncedEditState<T>
  ) {
    if (props.state !== state.previous) {
      return {
        previous: props.state,
        context: props.state,
      };
    } else {
      return state;
    }
  }
  commitState(t: T) {
    this.props.onCommit(t);
  }
  updateState(value: A) {
    const context = this.props.mapper(value);
    this.setState({ context });
    this.commitState(context);
  }
  render() {
    return this.props.children([this.state.context, this.updateState]);
  }
}
