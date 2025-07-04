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
import { trackEvent } from '../../../../analytics/ga.ts';
import { QuizAttempt, QuizInstructions, Rubric } from '../../../../api/quizApi.ts';
import RubricGrid from '../../../../components/rubric/RubricGrid.tsx';
import { QuizTimeLimit } from './QuizTimeLimit.tsx';
import ContentInstructions from '../../../parts/ContentInstructions';
import { useTranslation } from '../../../../i18n/translationContext.tsx';
import { statusValueSetterActionCreatorMaker } from '../../../../utilities/statusFlagReducer.ts';
import React, { useEffect } from 'react';
import { useCollapse } from 'react-collapsed';
import { FiInfo } from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

const instructionsToggleAction = statusValueSetterActionCreatorMaker({
  sliceName: 'instructionsState',
});

export const QuizInfoBar: React.FC<{
  instructions: QuizInstructions;
  rubric?: Rubric | null;
  attempt?: QuizAttempt;
  className?: string;
}> = ({ instructions, attempt, rubric, className }) => {
  const { getCollapseProps, getToggleProps, isExpanded } = useCollapse({
    defaultExpanded: false,
    id: 'quiz-instructions',
    onTransitionStateChange: transition => {
      if (transition.endsWith('Start')) {
        trackEvent(
          'Instructions Bar',
          transition === 'expandStart' ? 'Show Instructions' : 'Hide Instructions'
        );
      }
    },
  });
  const translate = useTranslation();

  const dispatch = useDispatch();
  useEffect(() => {
    dispatch(instructionsToggleAction({ instructions, rubric }));
    return () => {
      dispatch(instructionsToggleAction(undefined));
    };
  }, [instructions, rubric]);

  // display unset lets the timer be sticky :shrug:
  return (
    <>
      <div style={{ display: 'unset' }}>
        {attempt?.deadline && <QuizTimeLimit attempt={attempt} />}
        <Button
          color="link"
          className={classNames(
            'py-2 ps-2 pe-3 instructions-toggle text-nowrap d-print-none',
            className
          )}
          {...getToggleProps()}
        >
          <FiInfo className="me-2" />
          {translate(isExpanded ? 'HIDE_INSTRUCTIONS' : 'SHOW_INSTRUCTIONS')}
        </Button>
      </div>

      <div {...getCollapseProps()}>
        <div className={classNames('instructions-holder', className)}>
          <ContentInstructions instructions={instructions} />
        </div>
        {rubric && (
          <div className="mx-3 pb-3">
            <RubricGrid rubric={rubric} />
          </div>
        )}
      </div>
    </>
  );
};
