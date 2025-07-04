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
import Course from '../../../bootstrap/course';
import { useTranslation } from '../../../i18n/translationContext';
import { selectContentItems } from '../../../selectors/contentItemSelectors';
import { useCourseSelector } from '../../../loRedux';
import { resetQnaQuery, setQnaQuestions } from '../../../qna/qnaActions';
import { fetchQnaQuestions } from '../../../qna/qnaApi';
import QnaLink from '../../../qna/QnaLink';
import { QnaPageLink } from '../../../utils/pageLinks';
import LoadingSpinner from '../../../directives/loadingSpinner';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Card, CardBody, CardHeader } from 'reactstrap';

type UnansweredRow = {
  learningPathIndex: number;
  contentName: string;
  firstQuestionId: number;
  numberOfQuestions: number;
};

const UnansweredQna = () => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const contents = useCourseSelector(selectContentItems);

  const [loaded, setLoaded] = useState(false);
  const [count, setCount] = useState(0);
  const [unanswered, setUnanswered] = useState(new Array<UnansweredRow>());

  useEffect(() => {
    if (!Object.keys(contents).length) return;
    fetchQnaQuestions({
      filter: [{ property: 'open', operator: 'eq', value: true }],
      order: { property: 'created', direction: 'asc' },
    }).then(({ objects: openQuestions, count }) => {
      const groupedQuestions: Record<string, UnansweredRow> = openQuestions.reduce(
        (acc, question) => {
          if (acc[question.edgePath]) {
            acc[question.edgePath].numberOfQuestions += 1;
          } else {
            const content = contents[question.edgePath] ?? {
              learningPathIndex: Number.MAX_SAFE_INTEGER,
              name: Course.name,
            };
            acc[question.edgePath] = {
              learningPathIndex: content.learningPathIndex,
              contentName: content.name,
              firstQuestionId: question.id,
              numberOfQuestions: 1,
            };
          }
          return acc;
        },
        {} as Record<string, UnansweredRow>
      );
      const sortedDtos = Object.values(groupedQuestions).sort(
        (a, b) => a.learningPathIndex - b.learningPathIndex
      );
      setCount(count);
      setUnanswered(sortedDtos.slice(0, 5));
      setLoaded(true);
    });
  }, [contents]);

  return (
    <Card className="card-list">
      <CardHeader>
        <div className="d-flex flex-row-content">
          {loaded ? (
            <span className="circle-badge badge-primary ms-1">{count}</span>
          ) : (
            <LoadingSpinner />
          )}
          <span className="flex-grow-1">{translate('INSTRUCTOR_DASHBOARD_QNA_TITLE')}</span>
          <Link
            className="btn btn-sm btn-primary"
            to={QnaPageLink.toLink()}
            onClick={() => dispatch(resetQnaQuery())}
          >
            {translate('INSTRUCTOR_DASHBOARD_QNA_VIEW_ALL')}
          </Link>
        </div>
      </CardHeader>
      <CardBody className={classNames({ 'p-0': unanswered.length > 0 })}>
        {unanswered.length > 0 ? (
          <ul className="card-list-striped-body">
            {unanswered.map(row => (
              <li
                key={row.learningPathIndex}
                className="p-0"
              >
                <QnaLink
                  questionId={row.firstQuestionId}
                  className="flex-row-content"
                  style={{ padding: '0.75rem 1.25rem' }}
                  onClick={() => dispatch(setQnaQuestions([]))}
                >
                  <span className="flex-grow-1 flex-shrink-1">{row.contentName}</span>
                  <span className="badge badge-primary badge-pill">{row.numberOfQuestions}</span>
                </QnaLink>
              </li>
            ))}
          </ul>
        ) : (
          <div className="alert mb-0 alert-success">
            {translate('INSTRUCTOR_DASHBOARD_QNA_EMPTY')}
          </div>
        )}
      </CardBody>
    </Card>
  );
};
export default UnansweredQna;
