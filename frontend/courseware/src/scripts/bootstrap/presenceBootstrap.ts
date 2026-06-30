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

import type { Dispatch } from 'redux';

import { loadQuizActivityActionCreator } from '../courseActivityModule/actions/quizActivityActions';
import { courseReduxStore, type CourseState } from '../loRedux';
import { overallGradeMerge } from '../loRedux/overallGradeByUser';
import { presenceService } from '../presence/presenceServiceImpl';
import contentsResource from '../resources/ContentsResource';
import { scormCheckStateAction } from '../scorm/actions';
import { selectContentItems } from '../selectors/contentItemSelectors';
import { sessionEvents } from '../services/sessionEvents';
import { createDataListUpdateMergeAction } from '../utilities/apiDataActions';
import { COURSE_ROOT } from '../utilities/courseRootType';
import { currentUser } from '../utilities/currentUserData';
import { selectCurrentUser } from '../utilities/rootSelectors';
import Course from './course';

// sync with ContentGateNotification.scala
const ContentGateNotificationSchema = 'contentGateNotification';

// sync with AutoSubmitAttemptNotification.scala
const AutoSubmitAttemptNotificationSchema = 'autoSubmitAttemptNotification';
const AttemptInvalidatedNotificationSchema = 'attemptInvalidatedNotification';

/**
 * React-side re-home of the presence bootstrap side-effects that used to live in
 * five AngularJS `.run` blocks (`lo.bootstrap.presence` plus the gating / grade /
 * progress / autoSubmitAttempt notification listeners). The backing services are
 * all pure-TS singletons now, so the wiring is just direct calls.
 *
 * Called once from a React bootstrap effect (ERAppRoot), this drives the same
 * side-effects in the same order:
 *   1. `presenceService.init({ course })`
 *   2. session-event teardown wirings (`exit` / `logout` → stopServices)
 *   3. the four `presenceService.on('<Event>', …)` redux listeners
 *
 * A module-level run-once guard makes a double mount (StrictMode / remount) a no-op.
 */

let started = false;

export const startPresenceBootstrap = (): void => {
  if (started) return;
  started = true;

  // (1) presence init. Original gated on the injected `User` being present;
  // mirror that with the pure current-user singleton.
  if (currentUser) {
    // Defer past the synchronous bootstrap run phase: the presence singletons read Angular services
    // (Settings/PresenceAPI/...) lazily via `lojector`, which the `ple` module's run block sets
    // (initLo). By the next macrotask lojector is wired.
    setTimeout(
      () =>
        presenceService.init({
          course: Course.id,
        }),
      0
    );
  }

  // (2) session lifecycle teardown.
  sessionEvents.on('exit', () => presenceService.stopServices());
  sessionEvents.on('logout', () => presenceService.stopServices());

  // (3) gating notifications → open gate + invalidate contents.
  presenceService.on('Notification', ({ _type, contentId, student }) => {
    if (_type === ContentGateNotificationSchema) {
      courseReduxStore.dispatch(
        createDataListUpdateMergeAction('gatingInformationByContentByUser', {
          [student]: { [contentId]: { gateStatus: 'OPEN' } },
        })
      );
      contentsResource.invalidate().then();
    }
  });

  // (3) grade updates → grade-by-content / overall grade + scorm check.
  presenceService.on('GradeUpdate', ({ userId, edgePath, grade }) => {
    if (edgePath && edgePath !== COURSE_ROOT) {
      courseReduxStore.dispatch(
        createDataListUpdateMergeAction('gradeByContentByUser', {
          [userId]: { [edgePath]: grade },
        })
      );
    } else {
      courseReduxStore.dispatch(
        overallGradeMerge({
          [userId]: grade,
        })
      );
    }
    courseReduxStore.dispatch(scormCheckStateAction());
  });

  // (3) progress updates → contents transform + overall / by-content progress.
  presenceService.on('ProgressUpdate', ({ courseId, overallProgress, progressReport }) => {
    if (Course.id != courseId) {
      return;
    }
    const key = contentsResource.getKey(courseId, progressReport.userId);
    contentsResource.transform(key, progressReport);
    courseReduxStore.dispatch(
      createDataListUpdateMergeAction('overallProgressByUser', {
        [progressReport.userId]: overallProgress,
      })
    );
    courseReduxStore.dispatch(
      createDataListUpdateMergeAction('progressByContentByUser', {
        [progressReport.userId]: progressReport.progress,
      })
    );
  });

  // (3) auto-submit / attempt-invalidated notifications → reload the quiz activity.
  presenceService.on('Notification', ({ _type, topic }) => {
    if (
      _type === AutoSubmitAttemptNotificationSchema ||
      _type === AttemptInvalidatedNotificationSchema
    ) {
      courseReduxStore.dispatch((dispatch: Dispatch, getState: () => CourseState) => {
        const state = getState();
        const viewingAs = selectCurrentUser(state);
        const content = selectContentItems(state)[topic];
        dispatch(loadQuizActivityActionCreator(content, viewingAs, viewingAs.id));
      });
    }
  });
};
