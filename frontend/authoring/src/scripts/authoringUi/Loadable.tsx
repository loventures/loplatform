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

import React, { ComponentType, ReactNode } from 'react';

import Loading from './Loading';

type ErrFunc = (props: { error: Error }) => ReactNode;
type ErrComp = ComponentType<{ error: Error }>;

type FallbackType = ErrComp | ErrFunc | ReactNode | null;

interface LoadableProps {
  loading: boolean;
  className?: string;
  size?: string;
  style?: Record<any, any>;
  fallback?: FallbackType;
  children: () => ReactNode;
}

/**
 * This component will show the loading spinner until the loading prop is false.
 * The fallback prop is used if the error boundary is breached. It can be a component, a rendered node, or a
 *    function component. It accpets a single error prop.
 * You must use a nested arrow function to render the children.
 * */
class Loadable extends React.Component<LoadableProps> {
  state = { error: null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error, errorInfo) {
    console.error(error.message, errorInfo.componentStack);
    this.setState({ error });
  }

  render() {
    const { loading, children, style, className = '', size = '40px', fallback } = this.props;
    if (this.state.error) {
      if (React.isValidElement(fallback)) {
        return fallback;
      } else if (typeof fallback === 'function') {
        const fallbackRenderFunc = fallback as ErrFunc;
        return fallbackRenderFunc({ error: this.state.error });
      } else {
        const FallbackComponent = fallback as unknown as ErrComp;
        return <FallbackComponent error={this.state.error} />;
      }
    } else if (!loading && children) {
      return children();
    } else {
      return (
        <Loading
          loading={loading}
          className={className}
          style={style}
          size={size}
        />
      );
    }
  }
}

export default Loadable;
