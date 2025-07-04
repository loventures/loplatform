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

import Course from '../bootstrap/course';
import { useCourseSelector } from '../loRedux';
import { setQnaQuestions } from '../qna/qnaActions';
import { QnaQuestionDto } from '../qna/qnaApi';
import QnaLink from '../qna/QnaLink';
import { summarizeQnaQuestion } from '../qna/qnaUtil';
import { InstructorQnaPreviewPageLink } from '../utils/pageLinks';
import dayjs from 'dayjs';
import { selectContentItems } from '../selectors/contentItemSelectors';
import { history } from '../utilities/history';
import React, { useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { useTranslation } from '../i18n/translationContext';

type QnaQuestionRowProps = {
  question: QnaQuestionDto;
};
const QnaQuestionRow: React.FC<QnaQuestionRowProps> = ({ question }) => {
  const contents = useCourseSelector(selectContentItems);
  const content = contents[question.edgePath];
  const dispatch = useDispatch();
  const translate = useTranslation();

  const localOnClick = () => {
    dispatch(setQnaQuestions([question]));
    history.push(InstructorQnaPreviewPageLink.toLink(`${question.id}`));
  };

  const summary = useMemo(() => {
    const temp = document.createElement('span');
    return summarizeQnaQuestion(question, temp);
  }, [question]);

  const personName = !question.instructorMessage
    ? question.creator.fullName
    : !question.recipients?.length
      ? translate('QNA_ENTIRE_CLASS')
      : question.recipients.map(r => r.fullName).join(', ');

  return (
    <tr
      className="question-row"
      onClick={localOnClick}
    >
      <td>
        <div className="position-relative">
          <QnaLink
            questionId={question.id}
            onClick={e => {
              e.stopPropagation();
              dispatch(setQnaQuestions([question]));
            }}
            className="h5 d-block text-truncate mb-1"
          >
            {question.subject ?? content?.name ?? Course.name}
          </QnaLink>
          <div>
            <span className="text-muted">{personName}: </span>
            {summary}
            <span
              className="text-muted text-nowrap small float-right ms-2"
              style={{ lineHeight: '1.5rem' }}
            >
              {dayjs(question.created).format('MMM D, YYYY h:mm A')}
            </span>
          </div>
        </div>
      </td>
    </tr>
  );
};

export default QnaQuestionRow;
