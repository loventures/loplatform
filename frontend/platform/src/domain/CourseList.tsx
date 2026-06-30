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
import classNames from 'classnames';
import moment from 'moment-timezone';
import React, { useEffect, useState } from 'react';
import { Col, Input, InputGroup, InputGroupText, Label, Row } from 'reactstrap';

import encodeQuery from '../components/matrix';
import VisibilitySensor from '../components/VisibilitySensor';
import { useTranslations } from '../redux/state';
import { inCurrTimeZone } from '../services/moment';
import { CourseOverviewSectionUrl, CourseOverviewUrl } from '../services/URLs';

interface CourseInfo {
  id: number | string;
  courseGuid: string;
  courseName: string;
  projectName: string;
  startDate?: string | null;
  endDate?: string | null;
  shutdownDate?: string | null;
}

interface NextUp {
  id: string;
  name: string;
}

interface CourseEntry {
  course: CourseInfo;
  enrolledStudents?: number | null;
  startTime?: string | null;
  nextUp?: NextUp | null;
}

function entryMatches(entry: CourseEntry, str: string) {
  return (
    entry.course.courseGuid.toLowerCase().includes(str.toLowerCase()) ||
    entry.course.courseName.toLowerCase().includes(str.toLowerCase()) ||
    entry.course.projectName.toLowerCase().includes(str.toLowerCase())
  );
}

const CourseIcon: React.FC<{ className?: string }> = ({ className }) => {
  return (
    <svg
      className={className}
      strokeWidth="0"
      version="1.1"
      viewBox="0 0 17 17"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden
    >
      <g></g>
      <path d="M9 2v-2h-1v2h-7v10h15v-10h-7zM15 11h-13v-8h13v8zM3.5 13h10v1h-2.584l1.504 2.326-0.84 0.543-1.855-2.869h-0.725v3h-1v-3h-0.712l-1.869 2.87-0.838-0.545 1.514-2.325h-2.595v-1z"></path>
    </svg>
  );
};

const CourseList: React.FC = () => {
  const T = useTranslations();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [count, setCount] = useState(0);
  const [courses, setCourses] = useState<CourseEntry[]>([]);
  const [search, setSearch] = useState('');

  const formatDate = (date: string) => {
    const dateTimeFormat = T.t('format.date.full');
    return inCurrTimeZone(moment(date)).format(dateTimeFormat);
  };

  const loadMore = () => {
    setLoading(true);
    setCourses(prevCourses => {
      const offset = prevCourses.length;
      const matrix = encodeQuery(
        offset,
        30,
        { property: 'createTime', direction: 'desc' },
        [{ property: 'includeShutdownCourses', operator: 'eq', value: false }],
        []
      );
      axios
        .get(`${CourseOverviewUrl};${matrix}`)
        .then(({ data: { objects, filterCount } }) => {
          setLoading(false);
          setCourses(current => [...current.slice(0, offset), ...objects]);
          // the backend returns a false filterCount so stop at the end
          setCount(objects.length ? filterCount : prevCourses.length);
        })
        .catch(e => {
          console.log(e);
          setError(true);
          setLoading(false);
        });
      return prevCourses;
    });
  };

  useEffect(() => {
    loadMore();
     
  }, []);

  const onVisibility = (visible: boolean) => {
    if (visible) {
      loadMore();
    }
  };

  const filtered = courses.filter(entry =>
    search
      .split(/\s+/)
      .filter(s => s !== '')
      .every(w => entryMatches(entry, w))
  );
  return (
    <div
      id="course-list-page"
      className="container"
    >
      <Row>
        <Col>
          {(loading || courses.length > 0) && (
            <div className="d-flex flex-column my-4 mb-md-5 align-items-center">
              <InputGroup className={classNames('courseSearch', loading && 'loading')}>
                <Label
                  for="course-search"
                  className="visually-hidden"
                >
                  {T.t('page.courseList.searchForCourses')}
                </Label>
                <Input
                  id="course-search"
                  type="text"
                  placeholder={T.t('page.courseList.filterCourses')}
                  onChange={e => setSearch(e.target.value)}
                  readOnly={loading}
                />
                <InputGroupText className="material-icons">search</InputGroupText>
              </InputGroup>
            </div>
          )}
          {!loading && !courses.length && !error && (
            <div className="noCourses">{T.t('page.courseList.noCourses')}</div>
          )}
          {filtered.map((entry, idx) => {
            const ended =
              entry.course.endDate && !moment(entry.course.endDate).isAfter(moment());
            return (
              <div
                key={`${entry.course.id}-${idx}`}
                className={courses.length === 1 ? 'courseEntry mt-md-5' : 'courseEntry mb-4'}
              >
                <CourseIcon className="d-none d-md-inline-block" />
                <div className="flex-grow-1">
                  <a
                    href={`${CourseOverviewSectionUrl}/${entry.course.id}`}
                    className="courseLink"
                  >
                    <span className="courseTitle">{entry.course.courseName}</span>
                  </a>
                  <div className="courseProject">
                    <span className="prefix">{T.t('page.courseList.project')} </span>
                    <span className="value">{entry.course.projectName}</span>
                  </div>
                  <div className="courseGuid">
                    <span className="prefix">{T.t('page.courseList.section')} </span>
                    <span className="value">{entry.course.courseGuid}</span>
                  </div>
                  {entry.enrolledStudents && (
                    <div className="enrolledStudents">
                      <span className="prefix">{T.t('page.courseList.studentCount')} </span>
                      <span className="value">{entry.enrolledStudents}</span>
                    </div>
                  )}
                  {entry.startTime && (
                    <div className="enrollmentStart">
                      <span className="prefix">{T.t('page.courseList.enrolmentStart')} </span>
                      <span className="value">{formatDate(entry.startTime)}</span>
                    </div>
                  )}
                  {entry.course.startDate && (
                    <div className="startDate">
                      <span className="prefix">{T.t('page.courseList.startDate')} </span>
                      <span className="value">{formatDate(entry.course.startDate)}</span>
                    </div>
                  )}
                  {entry.course.endDate && (
                    <div className="endDate">
                      <span className="prefix">{T.t('page.courseList.endDate')} </span>
                      <span className="value">{formatDate(entry.course.endDate)}</span>
                    </div>
                  )}
                  {ended && (
                    <div className="shutdownDate">
                      {T.t('page.courseList.courseEnded', {
                        shutdownDate: formatDate(entry.course.shutdownDate!),
                      })}
                    </div>
                  )}
                  {entry.nextUp && !ended && (
                    <div className="nextUp">
                      <span className="prefix">{T.t('page.courseList.nextUp')} </span>
                      <a
                        className="value"
                        href={`${CourseOverviewSectionUrl}/${entry.course.id}?continue=student%2Fcontent%2F${entry.nextUp.id}`}
                      >
                        {entry.nextUp.name}
                      </a>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
          {!!courses.length && !filtered.length && (
            <div className="noMatch">{T.t('page.courseList.noMatch')}</div>
          )}
          {courses.length < count && !error && !loading && (
            <VisibilitySensor onChange={onVisibility}>
              <div id="visibility-sensor"></div>
            </VisibilitySensor>
          )}
          {loading && (
            <div
              id="loading-courses"
              className="courseEntry mt-md-5"
            >
              <CourseIcon className="d-none d-md-inline-block" />
              <div className="flex-grow-1">
                <a
                  href="/"
                  className="courseLink"
                >
                  <span className="courseTitle">Lorem Ipsum</span>
                </a>
                <div className="courseProject">
                  <span className="prefix">{T.t('page.courseList.project')} </span>
                  <span className="value">Lorem Ipsum Dolor</span>
                </div>
                <div className="courseGuid">
                  <span className="prefix">{T.t('page.courseList.section')} </span>
                  <span className="value">0xDEADBEEF</span>
                </div>
              </div>
            </div>
          )}
          {error && (
            <div
              id="loading-error"
              className="my-5"
            >
              {T.t('error.unexpectedError')}
            </div>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default CourseList;
