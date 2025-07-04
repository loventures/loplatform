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

/*
    This module provides utils for having rendering delayed until loaded.
    examples:

    const FooLoader = createLoaderComponent(selectFooLoadingState, loadFooAC, 'Foo')

    <FooLoader><Foo /><FooLoader>
    //this will render <Foo /> iff the loading state from selectFooLoadingState is loaded
    //in react dev tools the created <FooLoader /> will display as Loader(Foo)

    const LoadedFoo = wrapWithLoader(Foo, selectFooLoadingState, loadFooAC)
    //this has the exact same effect as above.
    //You just loose the ability to pass args to <Foo /> directly.
*/

import classNames from 'classnames';
import { CourseState } from '../loRedux';
import ErrorMessage from '../directives/ErrorMessage';
import LoadingSpinner from '../directives/loadingSpinner';
import { LoadingState } from './loadingStateUtils.ts';
import React, { ComponentType } from 'react';
import { connect } from 'react-redux';
import { ThunkAction } from 'redux-thunk';
import { Selector } from 'reselect';

type LoaderProps = {
  loadingState: {
    loading: boolean;
    loaded: boolean;
    error: any;
  };
  loadAction: (props: any) => any;
  useOverlay?: boolean;
  loaderKey?: string;
  LoadingMessage?: ComponentType;
  ErrorMessage?: ComponentType<{ error: any }>;
} & React.PropsWithChildren;

export class LoaderComponent extends React.Component<LoaderProps> {
  shouldRefresh() {
    return !this.props.loadingState.loading && !this.props.loadingState.loaded;
  }

  componentDidUpdate({ loaderKey: prevLoaderKey }: LoaderProps) {
    const { loaderKey } = this.props;
    if (prevLoaderKey !== loaderKey && this.shouldRefresh()) {
      this.props.loadAction(this.props);
    }
  }

  componentDidMount() {
    if (this.shouldRefresh()) {
      this.props.loadAction(this.props);
    }
  }

  render() {
    if (this.props.useOverlay) {
      return (
        <div className={classNames('overlay-loader', this.props.loadingState)}>
          {this.props.children}
          {this.props.loadingState.loading && (
            <div className="overlay-loader-overlay">
              <div className="alert alert-info my-4 text-center shadow">
                <LoadingSpinner message="LOADING_MESSAGE" />
              </div>
            </div>
          )}
          {this.props.loadingState.error && (
            <div className="overlay-loader-overlay">
              <ErrorMessage error={this.props.loadingState.error} />
            </div>
          )}
        </div>
      );
    }

    if (this.props.loadingState.loading) {
      if (this.props.LoadingMessage) {
        return <this.props.LoadingMessage />;
      } else {
        return (
          <div className="container loader-loading">
            <div className="alert alert-info my-4 text-center">
              <LoadingSpinner message="LOADING_MESSAGE" />
            </div>
          </div>
        );
      }
    }

    if (this.props.loadingState.error) {
      if (this.props.ErrorMessage) {
        return <this.props.ErrorMessage error={this.props.loadingState.error} />;
      } else {
        return <ErrorMessage error={this.props.loadingState.error} />;
      }
    }

    if (this.props.loadingState.loaded) {
      return this.props.children;
    }

    return <div className="loader-init"></div>;
  }
}

/*
    Creates a loader component,
    a component that delays rendering its children
    based on a selector that should include a "loadingState" prop
    whos shape is identical to that in ../../js/utilities/loadingStateUtils

    @param loadingStateSelector {function}
        a selector that selects something that includs a loadingState prop
    @param loadPageActionCreator {function}
        action creator for the action to load for the loading state above
        it should take the same argument as those specified on the resulting component
    @param displayName {string} optional
        display name for the resulting loader component. for debugging only.
    @param alwaysRefresh {boolean} optional
        should we always kick off the load action upon mount.
    @return {Component}
        A LoaderComponent. All props it receives will be given to the action creator.
*/
export const createLoaderComponent = (
  loadingStateSelector: Selector<CourseState, LoadingState>,
  loadPageActionCreator: () => ThunkAction<any, any, any, any>,
  displayName: string,
  alwaysRefresh = false
) => {
  class SpecificLoaderComponent extends LoaderComponent {
    static displayName = `Loader(${displayName})`;

    shouldRefresh() {
      return super.shouldRefresh() || (alwaysRefresh && !this.props.loadingState.loading);
    }
  }

  return connect(loadingStateSelector, { loadAction: loadPageActionCreator })(
    SpecificLoaderComponent
  );
};
