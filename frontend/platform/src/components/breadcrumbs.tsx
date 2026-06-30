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

/*
 * In-repo replacement for the `react-breadcrumbs` package, which only supports
 * react-router v4/v5. The original kept an internal redux store: each <Breadcrumb>
 * registers its data on mount, updates it when the data changes, and removes it on
 * unmount; <Breadcrumbs> subscribes to that store and renders the crumbs (shortest
 * pathname first) as NavLinks. We keep that contract and the exact BEM markup/classes
 * so existing styles and Selenide selectors keep working, swapping only the v5 NavLink
 * for the v6 one (`end` + className callback in place of `exact`/`activeClassName`).
 */

import React, { useEffect, useRef, useSyncExternalStore } from 'react';
import { NavLink } from 'react-router-dom';
import { isEqual } from 'underscore';

export interface CrumbData {
  title?: string;
  pathname: string;
  search?: string | null;
  state?: unknown;
}

interface Crumb extends CrumbData {
  id: string;
}

let crumbs: Crumb[] = [];
const listeners = new Set<() => void>();

const emit = () => {
  for (const l of listeners) l();
};

const addCrumb = (crumb: Crumb) => {
  crumbs = [...crumbs, crumb];
  emit();
};

const updateCrumb = (crumb: Crumb) => {
  crumbs = crumbs.map(c => (c.id === crumb.id ? crumb : c));
  emit();
};

const removeCrumb = (id: string) => {
  crumbs = crumbs.filter(c => c.id !== id);
  emit();
};

const subscribe = (listener: () => void) => {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};

const getSnapshot = () => crumbs;

let uid = 0;
const nextId = () => `crumb-${++uid}`;

interface BreadcrumbProps {
  data: CrumbData;
  hidden?: boolean;
  children?: React.ReactNode;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ data, hidden = false, children = null }) => {
  const idRef = useRef<string>('');
  if (!idRef.current) idRef.current = nextId();
  const id = idRef.current;
  const prev = useRef<{ data: CrumbData; hidden: boolean }>({ data, hidden });

  // Register on mount, deregister on unmount.
  useEffect(() => {
    if (!hidden) addCrumb({ id, ...data });
    return () => removeCrumb(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Reconcile on prop changes (mirrors the original componentWillReceiveProps).
  useEffect(() => {
    const p = prev.current;
    if (p.hidden === hidden) {
      if (!hidden && !isEqual(data, p.data)) updateCrumb({ id, ...data });
    } else if (hidden) {
      removeCrumb(id);
    } else {
      addCrumb({ id, ...data });
    }
    prev.current = { data, hidden };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hidden, data.title, data.pathname, data.search]);

  return <>{children}</>;
};

const block = 'breadcrumbs';

interface BreadcrumbsProps {
  className?: string;
  hidden?: boolean;
  separator?: React.ReactNode;
  setCrumbs?: (crumbs: Crumb[]) => Crumb[];
  wrapper?: React.ComponentType<{ className?: string; children?: React.ReactNode }>;
  children?: React.ReactNode;
}

const defaultWrapper: React.FC<{ className?: string; children?: React.ReactNode }> = props => (
  <nav {...props}>{props.children}</nav>
);

export const Breadcrumbs: React.FC<BreadcrumbsProps> = ({
  className = '',
  hidden = false,
  separator = '>',
  setCrumbs,
  wrapper: Wrapper = defaultWrapper,
  children,
}) => {
  const snapshot = useSyncExternalStore(subscribe, getSnapshot);
  const hiddenMod = hidden ? `${block}--hidden` : '';

  let list = [...snapshot].sort((a, b) => a.pathname.length - b.pathname.length);
  if (setCrumbs) list = setCrumbs(list);

  return (
    <div className={className}>
      <Wrapper className={`${block} ${hiddenMod}`}>
        <div className={`${block}__inner`}>
          {list.map((crumb, i) => (
            <span
              key={crumb.id}
              className={`${block}__section`}
            >
              <NavLink
                end
                className={({ isActive }) =>
                  isActive ? `${block}__crumb ${block}__crumb--active` : `${block}__crumb`
                }
                to={{ pathname: crumb.pathname, search: crumb.search || '' }}
                state={crumb.state}
              >
                {crumb.title}
              </NavLink>
              {i < list.length - 1 ? (
                <span className={`${block}__separator`}>{separator}</span>
              ) : null}
            </span>
          ))}
        </div>
      </Wrapper>
      {children}
    </div>
  );
};
