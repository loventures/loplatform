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
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { useCurrentAssetName } from '../../graphEdit';
import { useSurveyStats } from '../../story/dataActions';
import { toggleFeedbackOpen } from '../feedbackActions';

export const SurveySideButton: React.FC<{
  surveyed: boolean;
  disabled: boolean;
}> = ({ surveyed, disabled }) => {
  const dispatch = useDispatch();
  const assetName = useCurrentAssetName();
  const surveyStats = useSurveyStats();
  const count = surveyStats.responseStats[assetName];
  const onToggle = useCallback(() => {
    dispatch(toggleFeedbackOpen(true, 'survey'));
  }, []);

  return (
    <Button
      className={classNames('survey-toggle', 'feedback-icon', 'nice', !surveyed && 'grey')}
      style={{ marginTop: '4.5rem' }}
      onClick={onToggle}
      disabled={disabled}
      title="Survey"
    >
      <div className="material-icons md-24">{surveyed ? 'insights' : 'show_chart'}</div>
      {!!count && <div className="count2">{count > 9999 ? <>&infin;</> : count}</div>}
    </Button>
  );
};
