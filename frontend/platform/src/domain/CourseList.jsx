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
import moment from 'moment-timezone';
import React from 'react';
import { connect } from 'react-redux';
import VisibilitySensor from 'react-visibility-sensor';
import { Col, Input, InputGroup, InputGroupText, Label, Row } from 'reactstrap';
import { Spinner } from 'reactstrap';

import encodeQuery from '../components/matrix';
import LoPropTypes from '../react/loPropTypes';
import { inCurrTimeZone } from '../services/moment.js';
import { CourseOverviewSectionUrl, CourseOverviewUrl } from '../services/URLs';
import classNames from 'classnames';

function entryMatches(entry, str) {
  return (
    entry.course.courseGuid.toLowerCase().includes(str.toLowerCase()) ||
    entry.course.courseName.toLowerCase().includes(str.toLowerCase()) ||
    entry.course.projectName.toLowerCase().includes(str.toLowerCase())
  );
}

const CourseIcon = ({ className }) => {
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

class CourseList extends React.Component {
  state = {
    loading: true,
    error: false,
    count: 0,
    courses: [],
    search: '',
  };

  componentDidMount() {
    this.loadMore();
  }

  formatDate = date => {
    const { T } = this.props;
    const dateTimeFormat = T.t('format.date.full');
    return inCurrTimeZone(moment(date)).format(dateTimeFormat);
  };

  onVisibility = visible => {
    if (visible) {
      this.loadMore();
    }
  };

  loadMore = () => {
    this.setState({ loading: true });
    const offset = this.state.courses.length;
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
        this.setState(({ courses }) => ({
          loading: false,
          courses: [...courses.slice(0, offset), ...objects],
          count: objects.length ? filterCount : courses.length, // the backend returns a false filterCount so stop at the end
        }));
      })
      .catch(e => {
        console.log(e);
        this.setState({ error: true, loading: false });
      });
  };

  render() {
    const {
      state: { count, courses, error, loading, search },
      props: { T },
    } = this;
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
                <InputGroup className={classNames("courseSearch", loading && "loading")}>
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
                    onChange={e => this.setState({ search: e.target.value })}
                    readOnly={loading}
                  />
                  <InputGroupText className="material-icons">search</InputGroupText>
                </InputGroup>
              </div>
            )}
            {!loading && !courses.length && !error && (
              <div className="noCourses">
                {T.t('page.courseList.noCourses')}
              </div>
            )}
            {filtered.map((entry, idx) => {
              const ended = entry.course.endDate && !moment(entry.course.endDate).isAfter(moment());
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
                        <span className="value">{this.formatDate(entry.startTime)}</span>
                      </div>
                    )}
                    {entry.course.startDate && (
                      <div className="startDate">
                        <span className="prefix">{T.t('page.courseList.startDate')} </span>
                        <span className="value">{this.formatDate(entry.course.startDate)}</span>
                      </div>
                    )}
                    {entry.course.endDate && (
                      <div className="endDate">
                        <span className="prefix">{T.t('page.courseList.endDate')} </span>
                        <span className="value">{this.formatDate(entry.course.endDate)}</span>
                      </div>
                    )}
                    {ended && (
                      <div className="shutdownDate">
                        {T.t('page.courseList.courseEnded', {
                          shutdownDate: this.formatDate(entry.course.shutdownDate),
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
              <VisibilitySensor onChange={this.onVisibility}>
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
  }
}

/*
  Currently neglecting to display #enrolled students, progress, overall grade,
  link to download gradebook and modal with enroled students because...

  But for posterity this is how the old page processed this data.

        if (courseOverview.roleId) {
            courseOverview.data.isStudent =
                ['student', 'trialLearner'].indexOf(courseOverview.roleId) >= 0;
            courseOverview.data.isAdvisor = courseOverview.roleId === 'advisor';
            courseOverview.data.isInstructor = courseOverview.roleId === 'instructor';
            courseOverview.data.role = courseOverview.roleId;
        }

        if (courseOverview.stopTime) {
            courseOverview.data.enrollmentEndDate =
                formatMomentFilter(courseOverview.stopTime, 'full');
        }

        courseOverview.overallProgress = {
            ...courseOverview.overallProgress,
            title: courseOverview.course.courseName,
            normalizedProgress: courseOverview.overallProgress ? (
              courseOverview.overallProgress.weightedProgress / courseOverview.overallProgress.weightedTotal
            ) : 0,
        };

        if (courseOverview.nextUp) {
            const { iconCls, displayKey } = getContentDisplayInfo(courseOverview.nextUp);

            courseOverview.nextUp.iconCls = courseOverview.nextUp.iconCls || iconCls;
            courseOverview.nextUp.displayKey = displayKey;

            if (courseOverview.nextUp.duration) {
                courseOverview.nextUp.durationText = formatDurationFilter(moment.duration(+courseOverview.nextUp.duration, 'minutes'), 'short');
            }

            courseOverview.data.nextUpUrl =
                courseOverview.data.url +
                `#/${courseOverview.data.role}/content/${courseOverview.nextUp.id}`;
        }

        if (angular.isNumber(courseOverview.overallGrade)) {
            courseOverview.grade = {
                awarded: courseOverview.overallGrade,
                possible: 100
            };
        }

    <prog-circle
      class="me-3"
      prog="course.overallProgress"
      icon="course.course.iconCls">
    </prog-circle>

    <div class="course-info me-1">
      <h2 class="course-title" ng-bind="course.course.courseName"></h2>
      <div>
        <span translate="COURSE_ENROLLMENT"></span>
        <span ng-bind="course.data.role | translate"></span>
        <span ng-if="course.data.enrollmentEndDate"
          translate="COURSE_ENROLLMENT_END_DATE"
          translate-values="{endDate: course.data.enrollmentEndDate}">
        </span>
      </div>
      <div ng-if="course.course.endDate">
        <span translate="COURSE_END_DATE"></span>
        <span ng-bind="course.course.endDate | formatMoment:'full'"></span>
      </div>
    </div>

    <div>
      <span class="badge grade half-sized-percent"
        ng-if="course.grade"
        colored-grade-bg="course.grade"
        ng-bind="course.grade | grade:'percent'"
      ></span>
    </div>
*/

/*
0: {,…}
course: {endDate: null, url: "/Courses/blah", courseGuid: "blah", shutdownDate: null, id: 1561846375,…}
  endDate: null
  url: "/Courses/blah"
  courseGuid: "blah"
  shutdownDate: null
  id: 1561846375
  configuredShutdownDate: null
  startDate: null
  courseName: "blah"
enrolledStudents: null
overallGrade: null
roleId: "student"
nextUp: {duration: null, name: "Module 1 - with personalization", resourceType: null, assignmentType: null,…}
  duration: null
  name: "Module 1 - with personalization"
  resourceType: null
  assignmentType: null
  typeId: "module.1"
  id: "1561847403"
  contentType: "loi.cp.cbl.playlist.PlaylistContentItem"
overallProgress: {progressTypes: [], total: 131, progress: 0, weightedProgress: 0, completions: 0, weightedTotal: 131,…}
  progressTypes: []
  total: 131
  progress: 0
  weightedProgress: 0
  completions: 0
  weightedTotal: 131
  weightedPercentage: 0
stopTime: null
 null */

CourseList.propTypes = {
  T: LoPropTypes.translations,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
  };
}

export default connect(mapStateToProps, null)(CourseList);
