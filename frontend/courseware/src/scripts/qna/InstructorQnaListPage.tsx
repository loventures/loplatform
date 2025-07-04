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

import { isActualMatrixFilter, MatrixFilter, MatrixQuery } from '../bootstrap/loConfig';
import PaginationWithMax from '../components/PaginateWithMax';
import ERContentContainer from '../landmarks/ERContentContainer';
import { updateQnaQuery } from '../qna/qnaActions';
import { fetchQnaQuestions, QnaQuestionDto } from '../qna/qnaApi';
import { useQnaQuery } from '../qna/qnaHooks';
import QnaQuestionRow from '../qna/QnaQuestionRow';
import {
  QnaActiveFilter,
  QnaAnsweredFilter,
  QnaClosedFilter,
  QnaSentFilter,
} from '../qna/qnaReducer';
import { useTranslation } from '../i18n/translationContext';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { useDebounce } from 'react-use';
import {
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Input,
  Spinner,
  Table,
  UncontrolledButtonDropdown,
} from 'reactstrap';

import { InstructorQnaMulticastPageLink } from '../utils/pageLinks';
import ERNonContentTitle from '../commonPages/contentPlayer/ERNonContentTitle';

/***
 * Types of questions for an instructor:
 * Active: q.open && !q.closed
 * Answered: !q.open && !q.closed
 * Closed: q.closed
 * */
const InstructorQnaListPage: React.FC = () => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const matrixQuery = useQnaQuery();

  const [count, setCount] = useState(0);
  const [questions, setQuestions] = useState(new Array<QnaQuestionDto>());
  const [fetched, setFetched] = useState<MatrixQuery>({});

  useEffect(() => {
    fetchQnaQuestions(matrixQuery).then(({ objects: questions, totalCount }) => {
      setQuestions(questions);
      setCount(totalCount);
      setFetched(matrixQuery); // racy because we don't cancel prior requests
    });
  }, [matrixQuery]);

  const prefilters = useMemo(
    () =>
      (Array.isArray(matrixQuery.prefilter)
        ? matrixQuery.prefilter
        : [matrixQuery.prefilter]
      ).filter(isActualMatrixFilter),
    [matrixQuery]
  );

  const filter = prefilters.some(f => f.property === 'open' && f.value)
    ? 'active'
    : prefilters.some(f => f.property === 'closed' && f.value)
      ? 'closed'
      : prefilters.some(f => f.property === 'closed' && !f.value)
        ? 'answered'
        : 'sent';

  const changeFilter = useCallback(
    (filter: MatrixFilter[]) => {
      dispatch(
        updateQnaQuery({
          prefilter: [
            ...prefilters.filter(
              f => f.property !== 'open' && f.property !== 'closed' && f.property !== 'sent'
            ),
            ...filter,
          ],
          order: { property: 'created', direction: filter === QnaSentFilter ? 'desc' : 'asc' },
          offset: 0,
        })
      );
    },
    [prefilters]
  );

  const changeSearch = useCallback(
    (value: string) => {
      dispatch(
        updateQnaQuery({
          prefilter: [
            ...prefilters.filter(
              f => f.property === 'open' || f.property === 'closed' || f.property === 'sent'
            ),
            ...(value ? [{ property: 'messages', operator: 'ts', value }] : []),
          ],
          offset: 0,
        })
      );
    },
    [prefilters]
  );
  const [search, setSearch] = useState(
    () => (prefilters.find(f => f.property === 'messages')?.value as string) ?? ''
  );
  const prior = useRef(search);
  useDebounce(() => prior.current !== search && changeSearch((prior.current = search)), 333, [
    search,
  ]);

  const page = useCallback(
    (page: number) => {
      dispatch(updateQnaQuery({ offset: page * matrixQuery.limit! }));
    },
    [matrixQuery.limit]
  );

  return (
    <ERContentContainer title={translate('Questions')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('INSTRUCTOR_QNA_LIST_PAGE_TITLE')} />
            <div className="col p-4">
              <div className="d-flex text-nowrap pb-3 gap-3 justify-content-between">
                <Link
                  className="btn btn-primary"
                  to={InstructorQnaMulticastPageLink.toLink()}
                >
                  {translate('QNA_SEND_MESSAGE')}
                </Link>
                <UncontrolledButtonDropdown>
                  <DropdownToggle
                    color="primary"
                    caret
                    size="sm"
                  >
                    {translate(`QNA_TITLE_${filter}`)}
                  </DropdownToggle>
                  <DropdownMenu>
                    <DropdownItem onClick={() => changeFilter(QnaActiveFilter)}>
                      {translate('QNA_active')}
                    </DropdownItem>
                    <DropdownItem onClick={() => changeFilter(QnaAnsweredFilter)}>
                      {translate('QNA_answered')}
                    </DropdownItem>
                    <DropdownItem onClick={() => changeFilter(QnaSentFilter)}>
                      {translate('QNA_sent')}
                    </DropdownItem>
                    <DropdownItem onClick={() => changeFilter(QnaClosedFilter)}>
                      {translate('QNA_closed')}
                    </DropdownItem>
                  </DropdownMenu>
                </UncontrolledButtonDropdown>
                <Input
                  type="text"
                  placeholder="Search..."
                  value={search}
                  onChange={e => {
                    setFetched({});
                    setSearch(e.target.value);
                  }}
                />
              </div>

              <Table
                className="card-table border"
                striped
              >
                <tbody>
                  {fetched !== matrixQuery ? (
                    <LoadingRow />
                  ) : questions.length ? (
                    questions.map(question => (
                      <QnaQuestionRow
                        key={question.id}
                        question={question}
                      />
                    ))
                  ) : (
                    <EmptyRow />
                  )}
                </tbody>
              </Table>

              <div className="d-flex justify-content-end mt-3">
                <PaginationWithMax
                  pageIndex={matrixQuery.offset! / matrixQuery.limit!}
                  numPages={Math.ceil(count / matrixQuery.limit!)}
                  pageAction={pageIndex => page(pageIndex)}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default InstructorQnaListPage;

const EmptyRow: React.FC = () => {
  const translate = useTranslation();
  return (
    <tr>
      <td className="d-flex align-items-center justify-content-center">
        <span>{translate('No Questions')}</span>
      </td>
    </tr>
  );
};

const LoadingRow: React.FC = () => {
  return (
    <tr>
      <td className="d-flex align-items-center justify-content-center text-muted">
        <Spinner size="sm" />
      </td>
    </tr>
  );
};
