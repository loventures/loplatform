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

import { Content } from '../../../api/contentsApi.ts';
import { BasicScore, QuizAttempt, RubricScore, testOutDiagnostic } from '../../../api/quizApi.ts';
import {
  ContentWithAncestors,
  useLearningPathResource,
} from '../../../resources/LearningPathResource.ts';
import { isEmpty } from 'lodash';
import { QuizActivity } from './parts/QuizActivityInfo.tsx';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import React, { useEffect, useState } from 'react';
import { Alert, Button, Spinner } from 'reactstrap';
import { lojector } from '../../../loject.ts';

// The typescript types are a complete lie
const isActualScore = (score: BasicScore | RubricScore | null): score is BasicScore =>
  typeof (score as any)?.pointsAwarded === 'number';

const QuizTestOut: React.FC<{ quiz: QuizActivity; attempt: QuizAttempt }> = ({ quiz, attempt }) => {
  const testsOut = quiz.assessment.settings.testsOut;
  const score = attempt.score;
  const gradeFilter = lojector.get('gradeFilter') as any;
  const grade = isActualScore(score) ? score.pointsAwarded / score.pointsPossible : 0;
  const learningPath = useLearningPathResource();
  const translate = useTranslation();

  const testOuts: Record<string, boolean> = {}; // every test out content success/fail
  const testedOutContent = new Array<Content>(); // direct tested out content
  const testOutContent = (content: Content) => {
    const testOutGrade = testsOut[content.id];
    if (testOutGrade != null) {
      const testedOut = grade + 0.001 >= testOutGrade;
      testOuts[content.id] = testedOut;
      if (testedOut) testedOutContent.push(content);
    }
  };
  for (const module of learningPath.modules) {
    testOutContent(module.content);
    module.elements.forEach(testOutContent);
  }

  const testedOutFCContent = new Array<Content>(); // leaf tested out for credit items
  const testOutFCContent = (content: ContentWithAncestors) => {
    if (content.isForCredit) {
      const testedOut =
        testOuts[content.id] ??
        testOuts[content.lesson?.id ?? 'n/a'] ??
        testOuts[content.module?.id ?? 'n/a']; // closest test-out success/fail
      if (testedOut) testedOutFCContent.push(content);
    }
  };
  for (const module of learningPath.modules) {
    module.elements.forEach(testOutFCContent);
  }

  const alreadyTestedOut = testedOutContent.every(
    content => content.progress && content.progress.completions >= content.progress.total
  );
  const [state, setState] = useState<'' | 'InProgress' | 'Skip' | 'Wait' | 'Done' | 'Error'>('');
  const doTestOut = () => {
    setState('InProgress');
    testOutDiagnostic(attempt.id)
      .then(() => setState('Wait')) // wait for SSE to update
      .catch(e => {
        console.log(e);
        setState('Error');
      });
  };
  useEffect(() => {
    if (state === 'Wait' && alreadyTestedOut) {
      const timeout = setTimeout(() => setState('Done'), 500); // extra .5s for less flicker
      return () => clearTimeout(timeout);
    }
  }, [state, alreadyTestedOut]);

  return ((isEmpty(testedOutContent) || alreadyTestedOut) && !state) || state == 'Skip' ? null : (
    <Alert
      color="success"
      className="mt-4"
    >
      {state === 'Done' ? (
        <div>{translate('TEST_OUT_CONTENT_SKIPPED')}</div>
      ) : state === 'Wait' ? (
        <div className="d-flex justify-content-center">
          <Spinner color="success" />
        </div>
      ) : state === 'Error' ? (
        <div>{translate('TEST_OUT_ERROR_OCCURRED')}</div>
      ) : (
        <>
          <div
            className="font-weight-bold mb-2"
            style={{ fontSize: '1.1rem' }}
          >
            {translate('TEST_OUT_CONGRATULATIONS')}
          </div>
          <div className="mb-1">{translate('TEST_OUT_PREAMBLE')}</div>
          <div className="mb-2 ps-4">{testedOutContent.map(c => c.name).join(', ')}</div>
          {isEmpty(testedOutFCContent) ? (
            <div>{translate('TEST_OUT_NONE_FOR_CREDIT')}</div>
          ) : (
            <>
              <div className="mb-1">
                {translate('TEST_OUT_FOR_CREDIT', { percent: gradeFilter(score, 'percentSign') })}
              </div>
              <div className="mb-2 ps-4">{testedOutFCContent.map(c => c.name).join(', ')}</div>
              <div>{translate('TEST_OUT_COME_BACK')}</div>
            </>
          )}
          <div className="d-flex justify-content-center mt-3 mb-1">
            <div
              className="text-nowrap d-flex justify-content-end"
              style={{ width: 0 }}
            >
              <button
                key="skip"
                className="btn skip-quiz btn-link me-3 alert-success border-0"
                onClick={() => setState('Skip')}
              >
                {translate('TEST_OUT_IGNORE')}
              </button>
            </div>
            <Button
              color="success"
              onClick={doTestOut}
              disabled={!!state}
            >
              {translate('TEST_OUT_SKIP_CONTENT')}
            </Button>
          </div>
        </>
      )}
    </Alert>
  );
};

export default QuizTestOut;
