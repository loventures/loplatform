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
import { RiKeyLine } from 'react-icons/ri';
import { Link } from 'react-router-dom';

import { accessRightI18n, accessRightsMap } from '../../components/AccessRightEditor';
import { TreeAsset } from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { NodeName } from '../../types/asset';
import { getIcon, isContainer } from '../AddAsset';
import { ContentStatusPill } from '../badges/ContentStatusPill';
import { Stornado } from '../badges/Stornado';
import { useContentAccess } from '../hooks/useContentAccess';
import { editorUrl, plural } from '../story';

export const IndexRow: React.FC<{
  content: TreeAsset;
  branchId: number;
  regex: RegExp;
  questions: boolean;
  counts: Record<NodeName, number>;
  parameters: Record<string, any>;
}> = ({ content, branchId, regex, questions, counts, parameters }) => {
  const Icon = isContainer(content.typeId) || questions ? undefined : getIcon(content.typeId);

  const polyglot = usePolyglot();
  const accessRight = content.data.accessRight;
  const contentAccess = useContentAccess(content.name);

  const body = (
    <>
      {Icon && <Icon className="content-type-icon flex-shrink-0" />}
      <span
        className={classNames(
          'flex-shrink-1 text-truncate',
          contentAccess.ViewContent && 'hover-underline'
        )}
      >
        {questions ? (
          <>
            {`Question ${content.index + 1} – `}
            <span className="unhover-muted">{content.data.title || 'Untitled'}</span>
          </>
        ) : (
          content.data.title
        )}
      </span>
      <Stornado
        size="sm-no-ml"
        name={content.name}
      />
      {accessRight && (
        <RiKeyLine
          className={classNames(
            'access-restricted flex-shrink-0',
            'access-' + accessRightsMap[accessRight].toLowerCase()
          )}
          style={{ verticalAlign: '-.125rem' }}
          title={polyglot.t(accessRightI18n(accessRight))}
        />
      )}
      {!!counts[content.name] && contentAccess.ViewFeedback && (
        <span
          className="material-icons md-18 has-feedback flex-shrink-0"
          title={plural(counts[content.name], 'Feedback')}
        >
          chat_bubble_outline
        </span>
      )}
      <ContentStatusPill
        name={content.name}
        size="sm"
      />
    </>
  );
  return contentAccess.ViewContent ? (
    <Link
      to={editorUrl('story', branchId, content, content.context, parameters)}
      className={classNames(
        'story-index-item d-flex gap-2 align-items-center text-decoration-none',
        `story-nav-${content.typeId.replace(/\..*/, '')}`,
        `depth-${content.depth}`,
        regex.ignoreCase && regex.test(content.data.title) && 'hit'
      )}
    >
      {body}
    </Link>
  ) : (
    <div
      className={classNames(
        'story-index-item d-flex gap-2 align-items-center text-decoration-none bg-transparent',
        `story-nav-${content.typeId.replace(/\..*/, '')}`,
        `depth-${content.depth}`,
        regex.ignoreCase && regex.test(content.data.title) && 'hit'
      )}
    >
      {body}
    </div>
  );
};
