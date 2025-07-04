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
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import { RiFilterLine } from 'react-icons/ri';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { toggleFeedbackOpen } from '../feedbackActions';
import { useFeedbackFilters } from '../feedbackHooks';

export const FeedbackSideButton: React.FC<{ nice: boolean; count: number; disabled: boolean }> = ({
  nice,
  count,
  disabled,
}) => {
  const dispatch = useDispatch();
  const { branch, status, assignee, unit, module } = useFeedbackFilters();

  const onToggle = useCallback(() => {
    dispatch(toggleFeedbackOpen(true));
  }, []);

  return (
    <Button
      color={nice ? undefined : 'dark'}
      outline={!nice}
      className={classNames('feedback-toggle', 'feedback-icon', { nice })}
      onClick={onToggle}
      title="Feedback"
      disabled={disabled}
    >
      <div className="material-icons md-24">{count ? 'chat_bubble' : 'chat_bubble_outline'}</div>
      {!!count && <div className="count">{count > 99 ? <>&infin;</> : count}</div>}
      {assignee !== undefined || status !== 'Open' || unit || module || branch ? (
        <RiFilterLine className="feedback-filtered" />
      ) : null}
    </Button>
  );
};
