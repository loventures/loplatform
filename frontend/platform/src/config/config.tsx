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

import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';

import App from './components/ComponentsPage';
import useRouteBasePath from '../components/useRouteBasePath';
import { getSchemata, Schemata } from './configApi';

// Reads the :schema route param and (re)mounts the config form per schema, as the
// v5 code keyed <App> on the schema to force a remount when the selection changed.
const ConfigSchema = ({
  schemata,
  path,
  search,
}: {
  schemata: Schemata;
  path: string;
  search: string;
}) => {
  const { schema = '' } = useParams<{ schema: string }>();
  return (
    <App
      key={schema}
      schema={schema}
      schemata={schemata}
      path={path}
      search={search}
    />
  );
};

const Config = () => {
  const location = useLocation();
  const path = useRouteBasePath();
  const { search } = location;
  const [schemata, setSchemata] = useState<Schemata>({});
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    getSchemata().then(res => {
      setSchemata(res.data);
      setLoaded(true);
    });
  }, []);

  return (
    <div className="container-fluid">
      {loaded && (
        <Routes>
          <Route
            path=":schema"
            element={
              <ConfigSchema
                schemata={schemata}
                path={path}
                search={search}
              />
            }
          />
          <Route
            path="*"
            element={
              <Navigate
                to={{ pathname: path + '/' + Object.keys(schemata)[0], search }}
                replace
              />
            }
          />
        </Routes>
      )}
    </div>
  );
};

export default Config;
