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
import ERContentIndex from '../commonPages/contentPlayer/views/ERContentIndex';
import ERContentContainer from '../landmarks/ERContentContainer';
import { useCourseSelector } from '../loRedux';
import { setQnaQuestions } from '../qna/qnaActions';
import { fetchQnaQuestion } from '../qna/qnaApi';
import { useQnaQuestions } from '../qna/qnaHooks';
import QnaQuestionSidebar from '../qna/QnaQuestionSidebar';
import { useTranslation } from '../i18n/translationContext';
import { useContentItemWithRelations } from '../selectors/selectContentItemsWithNebulousDetails';
import { selectActualUser, selectCurrentUser } from '../utilities/rootSelectors';
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';

type InstructorQnaQuestionPageProps = {
  questionId: number;
};

const InstructorQnaQuestionPage: React.FC<InstructorQnaQuestionPageProps> = ({ questionId }) => {
  const viewingAs = useCourseSelector(selectCurrentUser);
  const actualUser = useCourseSelector(selectActualUser);
  const dispatch = useDispatch();
  const translate = useTranslation();

  const questions = useQnaQuestions();
  const question = questions[0];
  const content = useContentItemWithRelations(question?.edgePath);

  useEffect(() => {
    if (questionId !== question?.id) {
      fetchQnaQuestion(questionId).then(question => {
        dispatch(setQnaQuestions([question]));
      });
    }
  }, [questionId, question]);

  return (
    <>
      <ERContentContainer title={translate('INSTRUCTOR_QNA_LIST_PAGE_TITLE')}>
        {content && (
          <ERContentIndex
            content={content}
            actualUser={actualUser}
            viewingAs={viewingAs}
          />
        )}
        {!content && (
          <div className="container p-0">
            <div className="content-plain-index mb-3 m-md-3 m-lg-4">
              <div className="card er-content-wrapper">
                <div className="card-body p-4">
                  {question?.instructorMessage
                    ? question.subject
                    : translate('INSTRUCTOR_QNA_LIST_COURSE_QUESTION', {
                        student: question?.creator.fullName,
                        course: question?.subject ?? Course.name,
                      })}
                </div>
              </div>
            </div>
          </div>
        )}
      </ERContentContainer>
      {question && <QnaQuestionSidebar question={question} />}
    </>
  );
};

export default InstructorQnaQuestionPage;
