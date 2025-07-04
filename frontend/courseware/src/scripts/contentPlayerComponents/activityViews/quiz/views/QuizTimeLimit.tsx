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

import classNames from 'classnames';
import { QuizAttempt } from '../../../../api/quizApi.ts';
import { CourseState } from '../../../../loRedux';
import { useTranslation } from '../../../../i18n/translationContext.tsx';
import { submitQuizActionCreatorMaker } from '../../../../quizPlayerModule/actions/quizPlayerActions';
import { quizAttemptUnsavedChangesSelectorCreator } from '../../../../quizPlayerModule/selectors/quizPlayerSelectors';
import React, { useCallback, useEffect, useState } from 'react';
import { BsClock } from 'react-icons/bs';
import { useDispatch } from 'react-redux';
import { useSticky } from 'react-use-sticky';
import { Button } from 'reactstrap';
import { ThunkAction } from 'redux-thunk';

const autoSubmitAttemptActionCreatorMakerDoerBuilder =
  (attemptId: number): ThunkAction<void, CourseState, any, any> =>
  (dispatch, getState) => {
    const state = getState() as any as never; // what the fuck, Wes
    const { allUnsavedChanges } = quizAttemptUnsavedChangesSelectorCreator(attemptId)(state);
    dispatch(submitQuizActionCreatorMaker(attemptId)(allUnsavedChanges, -1, true));
  };

export const QuizTimeLimit: React.FC<{ attempt: QuizAttempt }> = ({ attempt }) => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const [ref, stuck] = useSticky<HTMLDivElement>();

  const computeRemaining = useCallback(() => {
    const ms = attempt.deadline!.diff(); // milliseconds left
    const delay = ms < 0 ? 0 : ms - Math.floor((ms - 1) / 60000) * 60000; // ms until the next clock UI transition
    return [ms <= 60000 ? 0 : Math.ceil(ms / 60000), delay];
  }, [attempt.deadline]);

  // a latch so we only submit once, the deadline may update during submission
  const [expired, setExpired] = useState(false);
  const [minutes, setMinutes] = useState(computeRemaining()[0]);
  const [hidden, setHidden] = useState(false);

  useEffect(() => {
    let timeout: NodeJS.Timeout | undefined = undefined;
    const updateTimeout = () => {
      const [minutes, delay] = computeRemaining();
      setMinutes(minutes);
      if (delay <= 0 && minutes <= 0) {
        setExpired(true);
      } else {
        timeout = setTimeout(updateTimeout, delay);
      }
    };
    updateTimeout();
    return () => clearTimeout(timeout);
  }, [computeRemaining]);

  useEffect(() => {
    if (expired) dispatch(autoSubmitAttemptActionCreatorMakerDoerBuilder(attempt.id));
  }, [expired, attempt.id]);

  return (
    <div
      ref={ref}
      className={classNames('quiz-timer-wrapper d-print-none', stuck && 'stuck')}
    >
      <div className="quiz-timer-gadget text-muted">
        <Button
          outline
          color="dark"
          title={translate(hidden ? 'TIMED_ASSESSMENT_SHOW_TIMER' : 'TIMED_ASSESSMENT_HIDE_TIMER')}
          className={classNames(
            'quiz-timer-toggle',
            hidden && 'hidden',
            !minutes && 'seconds-remaining'
          )}
          disabled={!stuck}
          onClick={() => setHidden(s => !s)}
          aria-expanded={!hidden}
          aria-controls="quiz-timer-remaining"
        >
          <BsClock />
        </Button>
        <div
          className={classNames(
            'pe-3 ps-1 text-muted flex-row align-items-center quiz-timer-message',
            hidden && stuck ? 'd-none' : 'd-flex'
          )}
          id="quiz-timer-remaining"
        >
          {translate('TIMED_ASSESSMENT_MINUTES', { minutes })}
        </div>
      </div>
    </div>
  );
};
