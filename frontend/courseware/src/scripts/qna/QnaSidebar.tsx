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
import { useCourseSelector } from '../loRedux';
import { setQnaQuestions, toggleQnaSideBar } from '../qna/qnaActions';
import { fetchQnaQuestion, fetchQnaQuestions } from '../qna/qnaApi';
import { useQnaQuestions } from '../qna/qnaHooks';
import { isEmpty } from 'lodash';
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors';
import { useTranslation } from '../i18n/translationContext';
import { selectCurrentUser, selectRouter } from '../utilities/rootSelectors';
import React, { useEffect, useRef, useState } from 'react';
import { TfiClose } from 'react-icons/tfi';
import { useDispatch } from 'react-redux';
import { useScroll } from 'react-use';
import { Button, Spinner } from 'reactstrap';

import { StudentQnaQuestionLink } from '../utils/pageLinks';
import AskQuestion from './AskQuestion';

const QnaSidebar: React.FC<{
  opened: boolean;
}> = ({ opened }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const close = () => dispatch(toggleQnaSideBar(undefined));

  const path = useCourseSelector(s => selectRouter(s).path);
  const instructorMessageMatch = StudentQnaQuestionLink.match(path);
  const questionId = instructorMessageMatch?.params.questionId;

  const headerRef = useRef<HTMLHeadingElement>(null);
  const messagesRef = useRef<HTMLDivElement>(null);
  const { y } = useScroll(messagesRef);

  const content = useCourseSelector(selectContent);
  const user = useCourseSelector(selectCurrentUser);

  const [loaded, setLoaded] = useState(false);
  const questions = useQnaQuestions();

  /**
   * Types of questions for a student:
   * Open: !q.closed
   * Response from Instructor: !q.closed && !q.open
   * Closed: q.closed
   * */
  const openQuestion = questions.find(q => q.instructorMessage || !q.closed);

  useEffect(() => {
    if (opened) {
      headerRef.current?.focus();
      setLoaded(false);

      let getter;

      if (questionId) {
        getter = fetchQnaQuestion(Number.parseInt(questionId, 10)).then(response => {
          return [response];
        });
      } else {
        getter = fetchQnaQuestions({
          prefilter: user.isPreviewing && { property: 'creator', operator: 'eq', value: user.id },
          filter: { property: 'edgePath', operator: 'eq', value: content.id },
          order: { property: 'created', direction: 'asc' },
        }).then(response => response.objects);
      }

      getter.then(response => {
        dispatch(setQnaQuestions(response));
        setLoaded(true);
      });
    }
  }, [content.id, questionId, opened]);

  const scrollToBottom = () => {
    const messageDiv = messagesRef?.current;
    if (messageDiv) {
      messageDiv.scrollTop = messageDiv.scrollHeight;
    }
  };

  return (
    <div
      id="QnaSidebar"
      className={classNames('qna-sidebar', { opened, scrolled: y > 0 })}
    >
      <div className="feedback-container">
        <div className="feedback-sidebar-header p-3 d-flex align-items-center">
          <Button
            color="medium"
            outline
            className="p-1 border-0 flex-grow-0 text-muted"
            style={{ lineHeight: 1 }}
            onClick={close}
            title={translate('QNA_SIDEBAR_CLOSE')}
            aria-label={translate('QNA_SIDEBAR_CLOSE')}
            disabled={!opened}
          >
            <TfiClose
              aria-hidden={true}
              size="1rem"
              style={{ strokeWidth: 0.75 }}
            />
          </Button>
          <h3
            ref={headerRef}
            tabIndex={-1}
            className="mb-0 text-center flex-grow-1 text-truncate"
          >
            {translate('QNA_SIDEBAR_TITLE')}
          </h3>
          <div
            className="d-none d-sm-block"
            style={{ width: '1.5rem' }}
          ></div>
        </div>
        <div
          className="d-flex flex-column overflow-auto mh-100"
          ref={messagesRef}
        >
          {opened &&
            (!loaded ? (
              <div className="text-muted text-center py-4">
                <Spinner size="sm" />
              </div>
            ) : (
              <>
                {questions.map(question => (
                  <AskQuestion
                    key={question.id}
                    content={content}
                    question={question}
                    onAddMessage={scrollToBottom}
                  />
                ))}
                {!openQuestion && (
                  <AskQuestion
                    content={content}
                    firstQuestion={isEmpty(questions)}
                    onAddMessage={scrollToBottom}
                  />
                )}
              </>
            ))}
        </div>
      </div>
    </div>
  );
};

export default QnaSidebar;
