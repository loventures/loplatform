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
import Course from '../bootstrap/course';
import ERNonContentTitle from '../commonPages/contentPlayer/ERNonContentTitle';
import ERContentContainer from '../landmarks/ERContentContainer';
import { useCourseSelector } from '../loRedux';
import { QnaQuestionDto, fetchQnaQuestions } from '../qna/qnaApi';
import { summarizeQnaQuestion } from '../qna/qnaUtil';
import {
  ContentPlayerPageLink,
  StudentQnaNewQuestionLink,
  StudentQnaQuestionLink,
} from '../utils/pageLinks';
import dayjs from 'dayjs';
import { partition } from 'lodash';
import { useTranslation } from '../i18n/translationContext';
import { selectContentItems } from '../selectors/contentItemSelectors';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React, { useEffect, useState } from 'react';
import { IoInformationCircleOutline } from 'react-icons/io5';
import { Link } from 'react-router-dom';
import { useDebounce } from 'react-use';
import { Card, CardBody, Input, Spinner, UncontrolledTooltip } from 'reactstrap';

import { COURSE_ROOT } from '../utilities/courseRootType';

type SummarizedQnaQuestion = QnaQuestionDto & {
  summary: string;
};

const LearnerQnaPage: React.FC = () => {
  const translate = useTranslation();
  const user = useCourseSelector(selectCurrentUser);
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');

  const [loaded, setLoaded] = useState(false);
  const [open, setOpen] = useState(new Array<SummarizedQnaQuestion>());
  const [closed, setClosed] = useState(new Array<SummarizedQnaQuestion>());

  useEffect(() => {
    setLoaded(false);
    fetchQnaQuestions({
      prefilter: [
        { property: 'creator', operator: 'eq', value: user.id },
        ...(query ? [{ property: 'messages', operator: 'ts', value: debouncedQuery }] : []),
      ],
      order: { property: 'created', direction: 'desc' },
    }).then(({ objects }) => {
      const temp = document.createElement('span');
      const summarized = objects.map(question => {
        const summary = summarizeQnaQuestion(question, temp);
        return { ...question, summary };
      });
      const [closed, open] = partition(summarized, q => q.closed);
      setOpen(open);
      setClosed(closed);
      setLoaded(true);
    });
  }, [user.id, debouncedQuery]);
  useDebounce(() => setDebouncedQuery(query), 333, [query]);

  return (
    <ERContentContainer title={translate('QNA_PAGE_TITLE')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('QNA_PAGE_TITLE')} />
            <div className="px-md-2 py-md-2 mb-last-0">
              <Link
                className="btn btn-primary mb-4"
                to={StudentQnaNewQuestionLink.toLink()}
              >
                Ask a Question
              </Link>
              <Input
                type="text"
                placeholder="Search..."
                className="mb-3"
                value={query}
                onChange={e => setQuery(e.target.value)}
              />
              {!loaded || query !== debouncedQuery ? (
                <div className="d-flex justify-content-center text-muted mt-4">
                  <Spinner size="sm" />
                </div>
              ) : (
                <>
                  {!!open.length && (
                    <h2 className="h4 mb-md-3">
                      Open{' '}
                      <IoInformationCircleOutline
                        id="open-id"
                        size=".85em"
                      />
                      <UncontrolledTooltip
                        target="open-id"
                        placement="right"
                        innerClassName="text-left"
                      >
                        <div>
                          <strong>Open Questions</strong>
                        </div>
                        <div className="small">
                          You're still waiting for an answer to these questions (or you haven't
                          confirmed your instructor's answer).
                        </div>
                      </UncontrolledTooltip>
                    </h2>
                  )}
                  {open.map(q => (
                    <QnaCard
                      key={q.id}
                      question={q}
                    />
                  ))}

                  {!!closed.length && (
                    <h2 className={classNames('h4 mb-md-3', open.length && 'mt-4')}>
                      Answered{' '}
                      <IoInformationCircleOutline
                        id="answered-id"
                        size=".85em"
                      />
                      <UncontrolledTooltip
                        target="answered-id"
                        placement="right"
                        innerClassName="text-left"
                      >
                        <div>
                          <strong>Answered Questions</strong>
                        </div>
                        <div className="small">
                          You have answers to these questions, but can still follow up if you need
                          to.
                        </div>
                      </UncontrolledTooltip>
                    </h2>
                  )}
                  {closed.map(q => (
                    <QnaCard
                      key={q.id}
                      question={q}
                    />
                  ))}

                  {!open.length && !closed.length && (
                    <div className="mt-4">
                      {translate(debouncedQuery ? 'QNA_PAGE_NO_MATCH' : 'QNA_PAGE_EMPTY')}
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

const QnaCard: React.FC<{
  question: SummarizedQnaQuestion;
}> = ({ question }) => {
  const contents = useCourseSelector(selectContentItems);
  const content = contents[question.edgePath];
  const to = content
    ? ContentPlayerPageLink.toLink({
        content: content ?? { id: COURSE_ROOT },
        qna: true,
      })
    : StudentQnaQuestionLink.toLink(question.id);
  return (
    <Card
      key={question.id}
      className={classNames('qna-row mb-2', question.closed && 'closed')}
    >
      <CardBody className="px-3 py-3">
        <Link
          to={to}
          className="h5 d-block text-truncate mb-1"
        >
          {question.subject ?? content?.name ?? Course.name}
        </Link>
        <div>
          {question.summary}
          <span
            className="text-muted text-nowrap small float-right ms-2"
            style={{ lineHeight: '1.5rem' }}
          >
            {dayjs(question.created).fromNow()}
          </span>
        </div>
      </CardBody>
    </Card>
  );
};

export default LearnerQnaPage;
