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

import React from 'react';
import { FormGroup, Input, Label } from 'reactstrap';

import { CourseConfig, CoursePreferences } from '../ProjectHistory/projectApi';

export const ProjectConfigurationForm: React.FC<{
  editMode?: boolean;
  config: CourseConfig;
  setOverrides: (f: (prefs: CoursePreferences) => CoursePreferences) => void;
}> = ({ editMode, config, setOverrides }) => {
  const { defaults, overrides } = config;

  return (
    <div
      className="d-flex flex-column gap-3 mt-3"
      style={{ marginLeft: '-1.5rem' }}
    >
      <Label
        className="gray-700 mb-0 ms-4"
        style={{ fontSize: '1.1rem' }}
      >
        Course Features
      </Label>
      <div className="d-flex align-items-center">
        <FormGroup check>
          <Input
            id="qna-check"
            type="checkbox"
            disabled={!editMode}
            checked={overrides.enableQna != null}
            onChange={() =>
              setOverrides(c => ({
                ...c,
                enableQna: c.enableQna == null ? true : undefined,
              }))
            }
          />
        </FormGroup>
        <FormGroup switch>
          <Label check>
            <Input
              id="qna-switch"
              type="switch"
              className={overrides.enableQna === false ? 'danger' : undefined}
              checked={!!(overrides.enableQna ?? defaults.enableQna)}
              disabled={!editMode || overrides.enableQna == null}
              onChange={() =>
                setOverrides(c => ({
                  ...c,
                  enableQna: !c.enableQna,
                }))
              }
            />
            Questions & answers
          </Label>
        </FormGroup>
      </div>
      <div className="d-flex align-items-center">
        <FormGroup check>
          <Input
            id="analytics-check"
            type="checkbox"
            disabled={!editMode}
            checked={overrides.enableAnalyticsPage != null}
            onChange={() =>
              setOverrides(c => ({
                ...c,
                enableAnalyticsPage: c.enableAnalyticsPage == null ? true : undefined,
              }))
            }
          />
        </FormGroup>
        <FormGroup switch>
          <Label check>
            <Input
              id="analytics-switch"
              type="switch"
              className={overrides.enableAnalyticsPage === false ? 'danger' : undefined}
              checked={!!(overrides.enableAnalyticsPage ?? defaults.enableAnalyticsPage)}
              disabled={!editMode || overrides.enableAnalyticsPage == null}
              onChange={() =>
                setOverrides(c => ({
                  ...c,
                  enableAnalyticsPage: !c.enableAnalyticsPage,
                }))
              }
            />
            Instructor analytics page
          </Label>
        </FormGroup>
      </div>
      <div className="d-flex align-items-center">
        <FormGroup check>
          <Input
            id="resources-check"
            type="checkbox"
            disabled={!editMode}
            checked={overrides.CBLPROD16934InstructorResources != null}
            onChange={() =>
              setOverrides(c => ({
                ...c,
                CBLPROD16934InstructorResources:
                  c.CBLPROD16934InstructorResources == null ? '' : undefined,
              }))
            }
          />
        </FormGroup>
        <Input
          type="text"
          id="instructor-resources"
          bsSize="sm"
          placeholder="Legacy instructor resources URL"
          disabled={!editMode || overrides.CBLPROD16934InstructorResources == null}
          value={
            overrides.CBLPROD16934InstructorResources ??
            defaults.CBLPROD16934InstructorResources ??
            ''
          }
          onChange={e =>
            setOverrides(c => ({
              ...c,
              CBLPROD16934InstructorResources: e.target.value,
            }))
          }
        />
      </div>
      <div className="d-flex align-items-center">
        <FormGroup check>
          <Input
            id="iac-check"
            type="checkbox"
            disabled={!editMode}
            checked={overrides.ltiISBN != null || overrides.ltiCourseKey != null}
            onChange={e =>
              setOverrides(c => ({
                ...c,
                ltiISBN: e.target.checked ? '' : undefined,
                ltiCourseKey: e.target.checked ? '' : undefined,
              }))
            }
          />
        </FormGroup>
        <Input
          type="text"
          id="lti-isbn"
          bsSize="sm"
          className="me-1"
          placeholder="IAC ISBN"
          disabled={!editMode || overrides.ltiISBN == null}
          value={overrides.ltiISBN ?? defaults.ltiISBN ?? ''}
          onChange={e =>
            setOverrides(c => ({
              ...c,
              ltiISBN: e.target.value,
            }))
          }
        />
        <Input
          type="text"
          id="lti-course-key"
          bsSize="sm"
          className="ms-1"
          placeholder="Course Key ('unset' to allow instructor override)"
          disabled={!editMode || overrides.ltiCourseKey == null}
          value={overrides.ltiCourseKey ?? defaults.ltiCourseKey ?? ''}
          onChange={e =>
            setOverrides(c => ({
              ...c,
              ltiCourseKey: e.target.value,
            }))
          }
        />
      </div>
    </div>
  );
};
