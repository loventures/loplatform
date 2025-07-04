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

import classnames from 'classnames';
import { useQuizApi } from '../../../../api/quizApi.ts';
import { ScrollToTopContext } from '../../../../landmarks/ScrollToTopProvider.tsx';
import { Competency } from '../../../../resources/CompetencyResource.ts';
import { useCourseSelector } from '../../../../loRedux';
import QuizActivityInfo from '../parts/QuizActivityInfo.tsx';
import { reportProgressActionCreator } from '../../../../courseActivityModule/actions/activityActions.ts';
import { quizActivityAfterInvalidateActionCreator } from '../../../../courseActivityModule/actions/quizActivityActions';
import { useTranslation } from '../../../../i18n/translationContext.tsx';
import { focusMainContent } from '../../../../utilities/focusUtils';
import { topicQuizzing } from '../../../../utilities/preferences.ts';
import React, { useContext, useState } from 'react';
import { useDispatch } from 'react-redux';
import Select from 'react-select';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import ContentInstructions from '../../../parts/ContentInstructions';

const QuizActivityPlayInstructions: React.FC<{
  quiz: any;
  content: any;
  viewingAs: any;
  printView: boolean;
  startPlaying: React.Dispatch<React.SetStateAction<{ topics?: string[] } | undefined>>;
}> = ({ quiz, content, viewingAs, printView, startPlaying }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const scrollToTop = useContext(ScrollToTopContext);
  const skipIt = () => {
    dispatch(reportProgressActionCreator(content, true, 'SKIPPED'));
  };
  const undo = () => {
    dispatch(reportProgressActionCreator(content, false, 'UNVISIT'));
  };
  const { invalidateAttempt } = useQuizApi();
  const skipped = content.progress?.progressTypes?.includes('SKIPPED');
  const skippable =
    !content.isForCredit && !quiz.hasSubmittedAttempts && !quiz.assessment.settings.isCheckpoint;

  const canDiscard =
    quiz.openAttempt &&
    quiz.canPlayAttempt &&
    window.lo_platform.course.groupType === 'PreviewSection';

  const competenciesByContent = useCourseSelector(s => s.api.competenciesByContent);
  const competencies: Array<{ value: string; label: string }> =
    competenciesByContent?.[content.id]?.map(({ nodeName, title }: Competency) => ({
      value: nodeName,
      label: title,
    })) ?? [];

  const [competency, setCompetency] = useState<string | undefined>();
  const selected = competencies.find(c => c.value === competency) ?? null;

  const [modalOpen, setModalOpen] = useState(false);

  const playQuiz = () => {
    startPlaying({ topics: competency ? [competency] : undefined });
    scrollToTop();
    focusMainContent();
  };

  const canPickTopic =
    !content.availability.isReadOnly &&
    !content.isForCredit &&
    quiz.canPlayAttempt &&
    !quiz.openAttempt &&
    quiz.assessment.settings.assessmentType === 'formative' &&
    !quiz.assessment.settings.maxAttempts &&
    !!competencies.length &&
    topicQuizzing;

  return (
    <div>
      <ContentInstructions instructions={quiz.assessment.instructions} />

      {!quiz.assessment.settings.isCheckpoint && (
        <QuizActivityInfo
          content={content}
          quiz={quiz}
        />
      )}

      {!printView && (
        <div className="flex-center-center mt-3 mb-0 py-2 py-md-3">
          {skipped ? (
            <>
              <span style={{ lineHeight: '3rem' }}>{translate('ASSESSMENT_SKIPPED')}</span>
              <button
                key="undo"
                className="btn btn-link p-0 ms-1 undo-skip"
                onClick={undo}
              >
                {translate('UNDO_SKIP_ASSESSMENT')}
              </button>
            </>
          ) : (
            <>
              {skippable && (
                <div
                  className="text-nowrap d-flex justify-content-end"
                  style={{ width: 0 }}
                >
                  <button
                    key="skip"
                    className="btn skip-quiz btn-link me-3"
                    disabled={content.availability.isReadOnly || !quiz.canPlayAttempt}
                    onClick={skipIt}
                  >
                    {translate('SKIP_ASSESSMENT')}
                  </button>
                </div>
              )}

              {canDiscard && (
                <button
                  key="discard"
                  className="btn discard-attempt btn-lg btn-outline-danger me-3"
                  onClick={() => {
                    invalidateAttempt(quiz.openAttempt.id).then(() => {
                      dispatch(
                        quizActivityAfterInvalidateActionCreator(
                          content,
                          quiz,
                          viewingAs,
                          viewingAs.id
                        )
                      );
                    });
                  }}
                >
                  {translate('ATTEMPT_DISCARD')}
                </button>
              )}

              {canPickTopic && (
                <Select
                  onChange={o => setCompetency(o?.value)}
                  value={selected}
                  options={competencies}
                  placeholder="Select topic..."
                  menuPosition="fixed"
                  isClearable
                  classNames={{ control: () => 'btn btn-large btn-success-outline text-left' }}
                  styles={{
                    container: css => ({ ...css, width: '16rem', marginRight: '1rem' }),
                    menuPortal: css => ({ ...css, zIndex: 2 }),
                  }}
                />
              )}

              <button
                key="start"
                className={classnames('btn start-quiz btn-lg', 'btn-success')}
                disabled={content.availability.isReadOnly || !quiz.canPlayAttempt}
                onClick={() => {
                  if (content.maxMinutes && !quiz.openAttempt) {
                    setModalOpen(true);
                  } else {
                    playQuiz();
                  }
                }}
              >
                {translate(quiz.openAttempt ? 'ASSESSMENT_RESUME' : 'TAKE_ASSESSMENT')}
              </button>
            </>
          )}
        </div>
      )}
      {content.maxMinutes ? (
        <Modal
          id="time-limit-modal"
          isOpen={modalOpen}
          toggle={() => setModalOpen(false)}
        >
          <ModalHeader>{translate('TIMED_ASSESSMENT_MODAL_TITLE')}</ModalHeader>
          <ModalBody>
            {translate('TIMED_ASSESSMENT_MODAL_BODY', { minutes: content.maxMinutes })}
          </ModalBody>
          <ModalFooter>
            <Button
              color="primary"
              outline
              onClick={() => setModalOpen(false)}
            >
              {translate('TIMED_ASSESSMENT_MODAL_CANCEL')}
            </Button>
            <Button
              color="primary"
              onClick={playQuiz}
            >
              {translate('TIMED_ASSESSMENT_MODAL_CONFIRM')}
            </Button>
          </ModalFooter>
        </Modal>
      ) : null}
    </div>
  );
};

export default QuizActivityPlayInstructions;
