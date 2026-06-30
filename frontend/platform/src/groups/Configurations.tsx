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

import axios from 'axios';
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { Alert } from 'reactstrap';

import ConfigApp from '../config/components/ComponentsPage';
import { getSchemata, Schemata } from '../config/configApi';
import useRouteBasePath from '../components/useRouteBasePath';

interface ConfigurationsProps {
  T: Polyglot;
  controllerValue: string;
  setLastCrumb: (title: string, documentTitle?: string) => void;
  warning?: string;
}

const Configurations: React.FC<ConfigurationsProps> = ({
  T,
  controllerValue,
  setLastCrumb,
  warning,
}) => {
  const [schemata, setSchemata] = useState<Schemata>({});
  const [loaded, setLoaded] = useState(false);
  const { courseId = '' } = useParams<{ courseId: string }>();
  const { search } = useLocation();
  const path = useRouteBasePath();

  useEffect(() => {
    axios.get(`/api/v2/${controllerValue}/${courseId}`).then(res => {
      setLastCrumb(T.t(`adminPage.${controllerValue}.configurations.name`, res.data));
    });
    getSchemata().then(({ data: { coursePreferences } }) => {
      setSchemata({ coursePreferences });
      setLoaded(true);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!loaded) return null;
  return (
    <div className="container">
      {warning && <Alert color="warning">{warning}</Alert>}
      <ConfigApp
        schema="coursePreferences"
        schemata={schemata}
        path={path}
        search={search}
        item={parseInt(courseId, 10)}
      />
    </div>
  );
};

export default Configurations;
