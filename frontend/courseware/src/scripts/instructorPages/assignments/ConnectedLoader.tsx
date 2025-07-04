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
import { FadeInLoader } from '../../directives/loadingSpinner/FadeInLoader';
import { RestError } from '../../error/RestError';
import { Loadable, isErrored, isLoading } from '../../types/loadable';
import * as React from 'react';
import { Dispatch, AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';

import { Connected } from './Connected';

export type ConnectedLoaderProps<T> = {
  /**
   * A function that 'picks' things out of CourseState and returns them
   */
  selector: (state: CourseState) => Loadable<T>;
  /**
   * A function run once when this component mounts.
   *
   * Use this to initiate fetches for the data you pick out of state
   */
  onMount: (state: CourseState, dispatch: ThunkDispatch<CourseState, void, AnyAction>) => void;
  /**
   * A function that takes the return value of selector, and returns the view.
   */
  children: (p: [T, Dispatch]) => React.ReactNode;
};

export class ConnectedLoader<T> extends React.Component<ConnectedLoaderProps<T>> {
  componentDidMount() {
    this.props.onMount(courseReduxStore.getState(), courseReduxStore.dispatch);
  }
  render() {
    return (
      <Connected selector={this.props.selector}>
        {([loadContext]) => {
          if (isLoading(loadContext)) {
            return (
              <div
                style={{
                  fontSize: '1.5rem',
                  width: '100%',
                  textAlign: 'center',
                  paddingTop: '4rem',
                }}
              >
                <FadeInLoader message="LOADING_MESSAGE" />
              </div>
            );
          } else if (isErrored(loadContext)) {
            return <RestError error={loadContext.error} />;
          } else {
            return this.props.children([loadContext.data, courseReduxStore.dispatch]);
          }
        }}
      </Connected>
    );
  }
}
