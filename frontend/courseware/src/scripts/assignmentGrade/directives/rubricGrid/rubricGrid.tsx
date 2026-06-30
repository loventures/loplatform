/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { find, findIndex, map } from 'lodash';
import React, { useMemo } from 'react';

import { Rubric } from '../../models/pure/rubric.ts';
import { RubricGridView } from './rubricGridView.tsx';

interface RubricGridProps {
  rubric: any;
  rubricResponse?: Record<string, { pointsAwarded?: number }>;
  rubricFeedback?: Array<{ sectionName: string; comment?: string }>;
}

/**
 * Maps the (new) rubric-response shape to the legacy per-section shape the Angular `Rubric` model
 * expects. Pure — unchanged from the old controller.
 */
const toLegacyRubricResponse = (rubric: any, rubricResponse: any, rubricFeedback: any) => {
  rubricResponse = rubricResponse || {};
  return map(rubric.sections, (section: any) => {
    const response = rubricResponse[section.name];
    const levelIndex = response && findIndex(section.levels, { points: response.pointsAwarded });
    const sectionFeedback = find(rubricFeedback, { sectionName: section.name });
    return {
      levelIndex,
      feedback: (sectionFeedback && sectionFeedback.comment) || '',
      levelGrade: response && response.pointsAwarded,
      manual: response && levelIndex === -1,
    };
  });
};

/**
 * React port of the `rubricGrid` directive (B2): builds an Angular `Rubric` model instance (the data
 * model stays Angular) from the raw rubric + response, then renders the already-native React
 * `RubricGridView` (#1462) directly — previously an Angular component bridged into React via
 * angular2react that rendered `<rubric-grid-view>`. Its only renderer is the React `DiscussionActivity`;
 * no Angular template mounts `<rubric-grid>`. Removing the bridge drops one angular2react bridge.
 */
export const RubricGrid: React.FC<RubricGridProps> = ({ rubric, rubricResponse, rubricFeedback }) => {
  const rubricInstance = useMemo(() => {
    const legacyRubricResponse = toLegacyRubricResponse(rubric, rubricResponse, rubricFeedback);
    const RubricModel = Rubric as new (rubric: any, response: any) => { sections: any[] };
    return new RubricModel(rubric, legacyRubricResponse);
  }, [rubric, rubricResponse, rubricFeedback]);

  return <RubricGridView rubric={rubricInstance} />;
};

