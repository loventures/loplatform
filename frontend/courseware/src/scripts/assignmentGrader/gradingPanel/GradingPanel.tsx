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

import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';

import { useGrader } from '../graderContext.tsx';

import { GradeFeedbackPanel } from '../../assignmentGrade/directives/gradeFeedbackPanel.tsx';
import { RubricGradePanel } from '../../assignmentGrade/directives/rubricGradePanel.tsx';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { useCourseSelector } from '../../loRedux';
import { AssessmentSubmissionSection } from '../assessmentGrader/AssessmentSubmissionSection.tsx';
import { AuthenticAssessmentSubmissionSection } from '../authenticAssessmentGrader/AuthenticAssessmentSubmissionSection.tsx';
import { graderStatusFlagToggleAction } from './graderActions.js';
import { GradingPanelControls } from './gradingPanelControls.tsx';
import { GradingPanelSection } from './GradingPanelSection.tsx';
import { GradingPanelStudentPicker } from './studentPicker.tsx';

interface GradingPanelProps {
  grader: any;
  onExit: () => void;
  /** 'quiz' (assessment grader w/ question dropdown) | 'submission' (student-driven) | 'authentic'. */
  variant: 'quiz' | 'submission' | 'authentic';
}

/**
 * React port of the `gradingPanel` Angular component (the instructor grader's right-hand panel). Was a
 * multi-slot-transclusion container (fixedTop/scrollableSections/fixedBottom); the fixed slots were
 * always empty and the scrollable slot only ever held the (now-React) submission section, so this is a
 * self-contained React component — no transclusion. It composes the React panel leaves directly
 * (student-picker, submission section, rubric, feedback, controls), dropping their react2angular kebabs.
 *
 * The grader (`QuizGrader`/`SubmissionGrader`) stays Angular and is mutated in place; re-renders are
 * driven by the shared `GraderProvider`/`useGrader` (one poll → version, B3 Phase 3) — consuming it here
 * re-renders the panel, which cascades to every leaf. Collapse state stays in redux
 * (`ui.graderOpenState`). DOM preserved from gradingPanel.html for the Selenide `GradingPanel` page
 * object: `.grading-panel-container`(`.panel-open`), `.grading-panel-expand-btn`,
 * `#grading-panel-content`, `.grading-panel-top`, `.grading-panel-scrollable-sections`,
 * `.grading-panel-bottom`.
 */
export const GradingPanel: React.FC<Omit<GradingPanelProps, 'grader'>> = ({ onExit, variant }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const { grader } = useGrader();
  const open = useCourseSelector((s: any) => s.ui.graderOpenState.status);
  const collapsed = !open;

  // Open on mount, close on unmount (the Angular $onInit/$onDestroy).
  useEffect(() => {
    dispatch(graderStatusFlagToggleAction(true));
    return () => {
      dispatch(graderStatusFlagToggleAction(false));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Passing undefined toggles (the statusFlag reducer flips when no explicit status is given).
  const togglePanel = () => dispatch(graderStatusFlagToggleAction(undefined));

  const submissionSection = () => {
    if (variant === 'authentic') {
      return (
        grader.activeUser && (
          <AuthenticAssessmentSubmissionSection
            grader={grader}
            user={grader.activeUser}
            attempt={grader.activeAttempt}
          />
        )
      );
    }
    if (variant === 'submission') {
      return (
        grader.activeUser && (
          <AssessmentSubmissionSection
            grader={grader}
            user={grader.activeUser}
            attempt={grader.activeAttempt}
          />
        )
      );
    }
    // 'quiz'
    return (
      grader.activeUser &&
      grader.activeAttempt && (
        <AssessmentSubmissionSection
          grader={grader}
          user={grader.activeUser}
          attempt={grader.activeAttempt}
          activeQuestions={grader.gradableQuestionList}
        />
      )
    );
  };

  return (
    <div className={`old-grader grading-panel-container${collapsed ? '' : ' panel-open'}`}>
      <section className="panel panel-right">
        <button
          className="grading-panel-expand-btn btn btn-primary p-0"
          onClick={togglePanel}
          aria-controls="grading-panel-content"
          aria-expanded={!collapsed}
        >
          <span className={`icon m-0 ${collapsed ? 'icon-chevron-left' : 'icon-chevron-right'}`} />
          <span className="sr-only">
            {translate(collapsed ? 'GRADING_PANEL_EXPAND' : 'GRADING_PANEL_COLLAPSE')}
          </span>
        </button>

        {!collapsed && (
          <div
            className="grading-panel-content"
            id="grading-panel-content"
            role="region"
            aria-label={translate('GRADING_PANEL_REGION')}
          >
            <div className="grading-panel-top">
              <GradingPanelStudentPicker grader={grader} />
            </div>

            <div className="grading-panel-scrollable-sections">
              <GradingPanelSection
                sectionTitle="GRADING_PANEL_SUBMISSION_HEADER"
                description="GRADING_PANEL_SUBMISSION_HEADER_DESC"
              >
                {submissionSection()}
              </GradingPanelSection>

              {grader.activeGrade?.outgoing?.rubric && (
                <GradingPanelSection
                  sectionTitle="GRADING_PANEL_RUBRIC_HEADER"
                  description="GRADING_PANEL_RUBRIC_HEADER_DESC"
                >
                  <RubricGradePanel
                    grade={grader.activeGrade}
                    disableEdit={!grader.canUserEditGrade}
                  />
                </GradingPanelSection>
              )}

              {grader.activeGrade && grader.detailedGradeExists && (
                <GradingPanelSection
                  sectionTitle="GRADING_PANEL_COMMENTS_HEADER"
                  description="GRADING_PANEL_COMMENTS_HEADER_DESC"
                >
                  <GradeFeedbackPanel
                    grade={grader.activeGrade}
                    disableEdit={!grader.canUserEditGrade}
                  />
                </GradingPanelSection>
              )}
            </div>

            <div className="grading-panel-bottom">
              <GradingPanelControls
                onExit={onExit}
                grade={grader.activeGrade}
                grader={grader}
              />
            </div>
          </div>
        )}
      </section>
    </div>
  );
};

