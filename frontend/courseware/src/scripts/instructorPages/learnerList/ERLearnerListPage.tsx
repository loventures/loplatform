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

import { UserInfo } from '../../../loPlatform';
import LearnerTableBulkActions from '../../instructorPages/learnerList/components/LearnerTableBulkActions';
import LearnerTableHeaders from '../../instructorPages/learnerList/components/LearnerTableHeaders';
import LearnerTableLoadingMessages from '../../instructorPages/learnerList/components/LearnerTableLoadingMessages';
import LearnerTableModal from '../../instructorPages/learnerList/components/LearnerTableModal';
import LearnerTablePagination from '../../instructorPages/learnerList/components/LearnerTablePagination';
import LearnerTableRows from '../../instructorPages/learnerList/components/LearnerTableRows';
import LearnerTableSearch from '../../instructorPages/learnerList/components/LearnerTableSearch';
import {
  setAllGrades,
  setAllProgress,
  setGrades,
  setProgress,
  setStudents,
} from '../../instructorPages/learnerList/learnerListActions';
import { loadLearnerListStudents } from '../../instructorPages/learnerList/learnerListLoad';
import learnerListReducer from '../../instructorPages/learnerList/learnerListReducer';
import {
  learnerListInitialState,
  OverallProgress,
  SrsArray,
} from '../../instructorPages/learnerList/learnerListStore';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { numericKeyBy } from '../../utils/utils';
import { useTranslation } from '../../i18n/translationContext';
import * as preferences from '../../utilities/preferences';
import React, { useEffect, useReducer } from 'react';
import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle';
import { lojector } from '../../loject';

type Grade = {
  user_id: number;
  grade: number;
  raw_grade: number;
  max: number;
};

const ERLearnerListPage: React.FC = () => {
  const translate = useTranslation();
  const [state, _dispatch] = useReducer(learnerListReducer, learnerListInitialState());

  // Load students when the filters change
  useEffect(() => {
    loadLearnerListStudents(state.filters, state.allGrades, state.allProgress).then(
      (students: SrsArray<UserInfo>) => {
        _dispatch(setStudents(state.filters, students));
      }
    );
  }, [state.filters, state.allGrades, state.allProgress]);

  // Load grades when the students change
  useEffect(() => {
    const students = state.students;
    if (students != null) {
      if (state.allGrades != null) {
        _dispatch(setGrades(students, state.allGrades));
      } else if (state.grades == null) {
        (lojector.get('GradebookAPI') as any)
          .getOverallGrades(
            students.map(student => student.id),
            undefined
          )
          .then((grades: SrsArray<Grade>) => {
            _dispatch(
              setGrades(
                students,
                numericKeyBy(grades, grade => grade.user_id)
              )
            );
          });
      }
    }
  }, [state.students, state.grades, state.allGrades]);

  // Load progress when the students change
  useEffect(() => {
    const students = state.students;
    if (students != null) {
      if (state.allProgress != null) {
        _dispatch(setProgress(students, state.allProgress));
      } else if (state.progress == null) {
        (lojector.get('ProgressService') as any)
          .getOverallProgressReportForLearners(
            students.map(student => student.id),
            undefined
          )
          .then((progress: Record<number, OverallProgress>) => {
            _dispatch(setProgress(students, progress));
          });
      }
    }
  }, [state.students, state.progress, state.allProgress]);

  // Load all grades when sorting by grade
  useEffect(() => {
    if (!state.allGrades && state.filters.sort.column === 'GRADE')
      (lojector.get('GradebookAPI') as any)
        .getOverallGrades(undefined, undefined)
        .then((grades: SrsArray<Grade>) => {
          _dispatch(setAllGrades(numericKeyBy(grades, grade => grade.user_id)));
        });
  }, [state.filters.sort.column, state.allGrades]);

  // Load all progress when sorting by progress
  useEffect(() => {
    if (
      !state.allProgress &&
      (state.filters.sort.column === 'PROGRESS' || state.filters.sort.column === 'ACTIVITY')
    )
      (lojector.get('ProgressService') as any)
        .getOverallProgressReportForLearners(undefined, undefined)
        .then((progress: Record<number, OverallProgress>) => {
          _dispatch(setAllProgress(progress));
        });
  }, [state.filters.sort.column, state.allProgress]);

  return (
    <ERContentContainer title={translate('STUDENT_LIST_PAGE_TITLE')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('STUDENT_LIST_PAGE_TITLE')} />
            <div className="col px-4">
              <div className="mb-4">
                <LearnerTableSearch
                  ariaControls="learner-table"
                  state={state}
                  _dispatch={_dispatch}
                />
              </div>

              <div className="student-competency-progress-table-page card mb-4">
                <div className="table-responsive">
                  <table
                    id="learner-table"
                    className="table table-striped card-table"
                  >
                    <LearnerTableHeaders
                      state={state}
                      _dispatch={_dispatch}
                    />
                    <LearnerTableRows
                      state={state}
                      _dispatch={_dispatch}
                    />
                  </table>
                </div>

                <LearnerTableLoadingMessages
                  className="card-body"
                  state={state}
                  _dispatch={_dispatch}
                />

                <div className="card-footer border-0">
                  <div className="flex-row-content flex-wrap">
                    {(preferences.allowDirectMessaging || preferences.instructorRoster) && (
                      <LearnerTableBulkActions
                        state={state}
                        _dispatch={_dispatch}
                      />
                    )}
                    <LearnerTablePagination
                      state={state}
                      _dispatch={_dispatch}
                    />
                  </div>
                </div>
              </div>

              {preferences.allowDirectMessaging && <LearnerTableModal />}
            </div>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERLearnerListPage;
