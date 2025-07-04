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
import { setQnaSummaries } from '../qna/qnaActions';
import { fetchQnaSummaries } from '../qna/qnaApi';
import QnaSidebar from '../qna/QnaSidebar';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';

const QnaContainer: React.FC = () => {
  const open = useCourseSelector(s => s.ui.qna.open);
  const dispatch = useDispatch();
  const user = useCourseSelector(selectCurrentUser);

  useEffect(() => {
    fetchQnaSummaries({
      prefilter: { property: 'creator', operator: 'eq', value: user.id },
    }).then(summaries => {
      dispatch(setQnaSummaries(summaries));
    });
  }, [user.id]);

  return <QnaSidebar opened={open} />;
};

export default QnaContainer;
