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

import { useCourseSelector } from '../loRedux';
import { replaceQnaQuestion, setQnaSummaries, updateQnaQuestion } from '../qna/qnaActions';
import { QnaQuestionDto, closeQuestion, fetchQnaSummaries } from '../qna/qnaApi';
import { useTranslation } from '../i18n/translationContext';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React from 'react';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

type CloseQuestionPromptProps = {
  question: QnaQuestionDto;
};
const CloseQuestionPrompt: React.FC<CloseQuestionPromptProps> = ({ question }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const user = useCourseSelector(selectCurrentUser);

  const reopen = () => {
    // Pretend the question is open so you can post a follow up message to reopen it...
    dispatch(replaceQnaQuestion({ ...question, open: true, reopened: true }));
  };

  const close = () => {
    closeQuestion(question.id).then(closedQuestion => {
      dispatch(updateQnaQuestion(closedQuestion));
      fetchQnaSummaries({
        prefilter: { property: 'creator', operator: 'eq', value: user.id },
      }).then(summaries => {
        dispatch(setQnaSummaries(summaries));
      });
    });
  };

  return (
    <div>
      <div className="d-flex justify-content-center my-3 text-warning small">
        {translate('QNA_DID_ANSWER')}
      </div>
      <div className="d-flex justify-content-end my-1 p-2">
        <Button
          size="sm"
          outline
          className="qna-close-question py-2 px-3"
          onClick={close}
          disabled={user.isPreviewing}
        >
          {translate('QNA_YES')}
        </Button>
        <Button
          size="sm"
          outline
          color="info"
          className="qna-close-question ms-2 py-2 px-3"
          onClick={reopen}
          disabled={user.isPreviewing}
        >
          {translate('QNA_NO')}
        </Button>
      </div>
    </div>
  );
};

export default CloseQuestionPrompt;
