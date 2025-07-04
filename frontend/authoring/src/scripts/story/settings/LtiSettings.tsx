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

import React, { useMemo } from 'react';

import { useAllEditedOutEdges, useEditedAssetTitle } from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { useLtiTools } from '../dataActions';
import { NarrativeSettings, plural, sentence } from '../story';
import { useIsEditable } from '../storyHooks';
import { AssessmentTypeEditor } from './components/AssessmentTypeEditor';
import { CategoryEditor } from './components/CategoryEditor';
import { CreditEditor } from './components/CreditEditor';
import { DurationEditor } from './components/DurationEditor';
import { PointsEditor } from './components/PointsEditor';

// Structural also offers:
// . Keywords
// . Icon
export const LtiSettings: NarrativeSettings<'lti.1'> = ({ asset }) => {
  const polyglot = usePolyglot();
  const editMode = useIsEditable(asset.name, 'EditSettings');
  const duration = asset.data.duration;
  const forCredit = asset.data.isForCredit;
  const allEdges = useAllEditedOutEdges(asset.name);
  const categoryEdge = allEdges.find(edge => edge.group === 'gradebookCategory');
  const categoryTitle = useEditedAssetTitle(categoryEdge?.targetName, 'Uncategorized');
  const points = asset.data.pointsPossible;
  const assessmentType = asset.data.assessmentType ?? 'formative';
  const toolList = useLtiTools();
  const tool = useMemo(
    () => toolList.find(o => o.toolId === asset.data.lti.toolId),
    [toolList, asset]
  );
  const defaultGraded = tool?.ltiConfiguration.defaultConfiguration.isGraded;
  const graded = tool?.ltiConfiguration.instructorEditable.isGraded
    ? (asset.data.lti.toolConfiguration.isGraded ?? defaultGraded)
    : defaultGraded;
  return editMode ? (
    <div className="mx-3 mb-3 text-center d-flex justify-content-center form-inline gap-2 parameter-center">
      {graded && (
        <>
          <CreditEditor
            asset={asset}
            forCredit={forCredit}
          />
          <CategoryEditor
            asset={asset}
            category={categoryEdge}
          />
          <PointsEditor
            asset={asset}
            points={points}
          />
        </>
      )}
      <AssessmentTypeEditor
        asset={asset}
        assessmentType={assessmentType}
      />
      <DurationEditor
        asset={asset}
        duration={duration}
      />
    </div>
  ) : (
    <div className="mx-3 mb-2 d-flex justify-content-center">
      <span className="input-padding text-muted text-center feedback-context">
        {sentence(
          graded && polyglot.t(`STORY_forCredit_${forCredit}`),
          graded && categoryTitle,
          graded && plural(points, 'point'),
          assessmentType,
          !duration ? 'no duration set' : plural(duration, 'minute')
        )}
      </span>
    </div>
  );
};
