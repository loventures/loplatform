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
import React, { useEffect } from 'react';
import { Route, Routes, useParams } from 'react-router-dom';

import Crumb from '../components/crumbRoute';
import CourseRightsTree from './CourseRightsTree';
import Roster from './Roster';
import UserEnrollments from './UserEnrollments';

interface EnrollmentsProps {
  T: Polyglot;
  controllerValue: string;
  sudoUrl: (course: any, user?: any) => string;
  includeRights: boolean;
  setLastCrumb: (title: string, documentTitle?: string) => void;
  readOnly: boolean;
}

const Enrollments: React.FC<EnrollmentsProps> = props => {
  const { setLastCrumb, T, controllerValue, sudoUrl, includeRights, readOnly } = props;
  const { courseId = '' } = useParams<{ courseId: string }>();

  useEffect(() => {
    axios.get(`/api/v2/${controllerValue}/${courseId}`).then(res => {
      setLastCrumb(T.t(`adminPage.${controllerValue}.enrollments.name`, res.data));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Routes>
      <Route
        path=""
        element={
          <Roster
            courseId={courseId}
            controllerValue={controllerValue}
            sudoUrl={sudoUrl}
            includeRights={includeRights}
            readOnly={readOnly}
          />
        }
      />
      {includeRights && (
        <Route
          path="Rights"
          element={
            <Crumb title={T.t('adminPage.enrollments.rightsTree.name')}>
              <CourseRightsTree courseId={courseId} />
            </Crumb>
          }
        />
      )}
      <Route
        path=":userId"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => (
              <UserEnrollments
                setLastCrumb={setLastCrumb}
                courseId={courseId}
                controllerValue={controllerValue}
              />
            )}
          </Crumb>
        }
      />
    </Routes>
  );
};

export default Enrollments;
