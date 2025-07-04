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
import React from 'react';
import { Link } from 'react-router-dom';

import { useEditedAssetTitle } from '../graphEdit';
import { useBranchId, usePolyglot } from '../hooks';
import { NodeName, TypeId } from '../types/asset';
import { Stornado } from './badges/Stornado';
import { isQuestion } from './questionUtil';
import { NarrativeMode, editorUrl, storyTypeName } from './story';

// This is also used for asset titles in feedback detail mode
export const QuestionTitle: React.FC<{
  name: NodeName;
  typeId: TypeId;
  contextPath: string;
  mode: NarrativeMode;
  commit?: number;
  className?: string;
}> = ({ name, typeId, contextPath, mode, commit, className }) => {
  const polyglot = usePolyglot();
  const branchId = useBranchId();
  const question = isQuestion(typeId);
  const assetTitle = useEditedAssetTitle(question ? undefined : name);
  const title = question ? storyTypeName(polyglot, typeId) : assetTitle;
  const fontSize = question ? '1.2rem' : '1.75rem';

  return (
    <div
      className={classNames('d-flex align-items-center justify-content-center', className)}
      data-id="title"
    >
      {mode === 'inline' || mode === 'feedback' ? (
        <Link
          className="asset-type unhover-muted text-center question-title"
          to={editorUrl('story', branchId, name, contextPath, { commit })}
        >
          <span
            className={question ? undefined : 'feedback-context'}
            style={{ fontSize }}
          >
            {title}
          </span>
          <div
            className="d-inline-block"
            style={{ width: 0, verticalAlign: question ? '2px' : '5px' }}
          >
            <Stornado name={name} />
          </div>
        </Link>
      ) : (
        <div className="asset-type text-muted text-center question-title">
          <span
            className={question ? undefined : 'feedback-context'}
            style={{ fontSize }}
          >
            {title}
          </span>
          <div
            className="d-inline-block"
            style={{ width: 0 }}
          >
            <Stornado name={name} />
          </div>
        </div>
      )}
    </div>
  );
};
