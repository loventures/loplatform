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
 * react-router v6 breadcrumb wrapper (the v5 <CrumbRoute> was a <Route> wrapper,
 * which no longer works as a child of <Routes>). Use it as a route element:
 *
 *   <Route path="/Foo/:id" element={<Crumb title={t}>{({ setLastCrumb }) =>
 *     <Foo setLastCrumb={setLastCrumb} />}</Crumb>} />
 *
 * It registers a breadcrumb for the current route, sets the document title, and hands
 * the page a `setLastCrumb` callback so async page data can rename the crumb. The crumb
 * pathname is the matched route URL (useResolvedPath('.')), matching the old match.url.
 */

import React, { useState } from 'react';
import DocumentTitle from 'react-document-title';
import { useLocation } from 'react-router-dom';

import { Breadcrumb } from './breadcrumbs';
import useRouteBasePath from './useRouteBasePath';

export type SetLastCrumb = (title: string, documentTitle?: string) => void;

interface CrumbProps {
  title?: string;
  documentTitle?: string;
  includeSearch?: boolean;
  children: React.ReactNode | ((api: { setLastCrumb: SetLastCrumb }) => React.ReactNode);
}

const Crumb: React.FC<CrumbProps> = ({
  title: initialTitle = '',
  documentTitle: initialDocumentTitle = '',
  includeSearch = false,
  children,
}) => {
  const [title, setTitle] = useState(initialTitle);
  const [documentTitle, setDocumentTitle] = useState(initialDocumentTitle);
  const location = useLocation();
  const url = useRouteBasePath();

  const setLastCrumb: SetLastCrumb = (newTitle, newDocumentTitle) => {
    setTitle(newTitle);
    setDocumentTitle(newDocumentTitle || '');
  };

  return (
    <DocumentTitle title={documentTitle || title}>
      <Breadcrumb
        hidden={!title}
        data={{
          title,
          pathname: url,
          search: includeSearch ? location.search : null,
        }}
      >
        {typeof children === 'function' ? children({ setLastCrumb }) : children}
      </Breadcrumb>
    </DocumentTitle>
  );
};

export default Crumb;
