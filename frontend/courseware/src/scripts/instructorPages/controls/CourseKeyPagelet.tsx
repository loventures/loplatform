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

import axios from 'axios';
import { useCourseSelector } from '../../loRedux';
import { ltiCourseKey } from '../../utilities/preferences';
import React, { useState } from 'react';
import { Button, Input, InputGroup } from 'reactstrap';

const CourseKeyPagelet: React.FC = () => {
  const section = useCourseSelector(s => s.course.id);

  const [working, setWorking] = useState(false);
  const [key, setKey] = useState(ltiCourseKey);

  const doSubmit = () => {
    setWorking(true);
    axios
      .put(`/api/v2/lwc/${section}/iac/courseKey`, key, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .then(() => {
        window.location.reload(); // lame, but reload prefs is not a thing
      })
      .finally(() => setWorking(false));
  };

  return (
    <div className="purge-pagelet d-flex flex-column align-items-center justify-content-center">
      <div className="alert alert-info m-4 instructor-instructions">
        <h2 className="h5">Course Key</h2>
        <p className="mb-0">Configure the course key for students to access remote resources.</p>
      </div>

      <div>
        <InputGroup>
          <Input
            className="text-monospace"
            type="text"
            value={key}
            onChange={e => setKey(e.target.value)}
            style={{ width: '20rem' }}
          />
          <Button
            color="primary"
            onClick={() => doSubmit()}
            disabled={working || !key || key === ltiCourseKey}
          >
            Save
          </Button>
        </InputGroup>
      </div>
    </div>
  );
};

export default CourseKeyPagelet;
