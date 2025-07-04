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

import * as React from 'react';
import { useMemo } from 'react';

import edgeRuleConstants from '../../editor/EdgeRuleConstants';
import { computeEditedTargets } from '../../graphEdit';
import { noGraphEdits } from '../../graphEdit/graphEditReducer';
import { usePolyglot } from '../../hooks';
import { getIcon, isContainer } from '../../story/AddAsset';
import { GenericEditor } from '../../story/editors';
import { narrativeEditors } from '../../story/NarrativeAsset/editors';
import { childEdgeGroup, storyTypeName } from '../../story/story';
import { ProjectGraph } from '../../structurePanel/projectGraphReducer';
import { NewAsset } from '../../types/asset';
import { NewAssetWithEdge } from '../../types/edge';

const isContainery = (asset: NewAsset<any>) => isContainer(asset.typeId);

const NarrativePreview: React.FC<{
  asset: NewAsset<any>;
  projectGraph: ProjectGraph;
}> = ({ asset, projectGraph }) => {
  const polyglot = usePolyglot();
  const NarrativeEditor = narrativeEditors[asset.typeId] ?? GenericEditor;
  const edgeGroup = childEdgeGroup(asset.typeId);
  const edgeRules = edgeRuleConstants[asset.typeId];
  const childBearing = !!edgeRules?.[edgeGroup];
  const containery = isContainery(asset);
  return (
    <>
      {containery && (
        <h4 className="text-center h5 mt-5">{storyTypeName(polyglot, asset.typeId)} Contents</h4>
      )}
      <NarrativeEditor
        asset={asset}
        context={[]}
        mode="inline"
        projectGraph={projectGraph}
        readOnly={true}
      />
      {childBearing && (
        <PreviewIndex
          asset={asset}
          projectGraph={projectGraph}
        />
      )}
    </>
  );
};

const PreviewIndex: React.FC<{
  asset: NewAsset<any>;
  projectGraph: ProjectGraph;
}> = ({ asset, projectGraph }) => {
  const group = childEdgeGroup(asset.typeId);
  const questions = group === 'questions';
  const children = useMemo(
    () => computeEditedTargets(asset.name, group, undefined, projectGraph, noGraphEdits),
    [asset, projectGraph]
  );

  return (
    <div className="content-list mx-5 my-5">
      {children.map(content => (
        <PreviewRow
          key={content.edge.name}
          content={content}
          questions={questions}
          projectGraph={projectGraph}
          depth={1}
        />
      ))}
    </div>
  );
};

const PreviewRow: React.FC<{
  content: NewAssetWithEdge<any>;
  projectGraph: ProjectGraph;
  questions: boolean;
  depth: number;
}> = ({ content, projectGraph, questions, depth }) => {
  const polyglot = usePolyglot();
  const Icon = isContainery(content) || questions ? undefined : getIcon(content.typeId);
  const children = useMemo(
    () => computeEditedTargets(content.name, 'elements', undefined, projectGraph, noGraphEdits),
    [content, projectGraph]
  );

  return (
    <>
      <div className={`story-index-item depth-${depth} d-flex align-items-center bg-transparent`}>
        {Icon && (
          <Icon
            className="text-muted me-1 flex-shrink-0"
            title={storyTypeName(polyglot, content.typeId)}
          />
        )}
        <div className="text-truncate flex-shrink-1 text-primary">
          {questions && <span className="text-muted">{`Question ${content.index + 1} – `}</span>}
          {content.data.title}
        </div>
      </div>
      {children.map(child => (
        <PreviewRow
          key={child.edge.name}
          content={child}
          questions={questions}
          projectGraph={projectGraph}
          depth={depth + 1}
        />
      ))}
    </>
  );
};

export default NarrativePreview;
