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
import { BsCalendarX } from 'react-icons/bs';
import { IoBookmarkOutline, IoCopyOutline, IoCutOutline, IoTrashOutline } from 'react-icons/io5';
import { RiKeyLine } from 'react-icons/ri';
import { Link } from 'react-router-dom';
import { Badge, Button } from 'reactstrap';

import { accessRightI18n, accessRightsMap } from '../../components/AccessRightEditor';
import { useFeedbackCount } from '../../feedback/feedbackHooks';
import { useEditedAsset, useRestoredAsset } from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import useIsMostRecent from '../../hooks/useMostRecent';
import { useProjectGraph } from '../../structurePanel/projectGraphActions';
import { NodeName } from '../../types/asset';
import { EdgeData } from '../../types/edge';
import { AddAsset, getIcon, isContainer } from '../AddAsset';
import { ContentStatusPill } from '../badges/ContentStatusPill';
import { Stornado } from '../badges/Stornado';
import { useContentAccess } from '../hooks/useContentAccess';
import { plural, storyTypeName } from '../story';
import { useIsEditable, useNarrativeAssetState, useRevisionCommit } from '../storyHooks';

const preventDefaults = (e: React.MouseEvent) => {
  e.preventDefault();
  e.stopPropagation();
};

export const IndexRow: React.FC<{
  index: number;
  name: NodeName;
  contextPath: string;
  parent: NodeName;
  edgeData: EdgeData<any>;
  questions: boolean;
  copyAsset: (event: React.MouseEvent) => void;
  removeAsset: (event: React.MouseEvent) => void;
  cutAsset: (event: React.MouseEvent) => void;
  unlinked: boolean;
}> = ({
  index,
  name,
  contextPath,
  parent,
  edgeData,
  questions,
  copyAsset,
  removeAsset,
  cutAsset,
  unlinked,
}) => {
  const polyglot = usePolyglot();
  const { branchId } = useProjectGraph();
  const canEdit = useIsEditable(parent, 'AddRemoveContent');
  const restored = !!useRestoredAsset(name);
  const content = useEditedAsset(name);
  const { created, deleted } = useNarrativeAssetState(name);
  const commit = useRevisionCommit();
  const commitQuery = commit ? `&commit=${commit}` : '';
  const isMostRecent = useIsMostRecent(parent, name);
  const feedbackCount = useFeedbackCount(name, true);
  const contentAccess = useContentAccess(name);
  const typeId = content?.typeId;
  const accessRight = content?.data.accessRight;

  const Icon = isContainer(typeId) || questions ? undefined : getIcon(typeId);

  return !content ? null : (
    <>
      <AddAsset
        parent={parent}
        contextPath={contextPath}
        before={content.name}
        className="mini-add"
        redirect
      />
      {unlinked || restored || !contentAccess.ViewContent ? (
        <div
          className={classNames(
            'story-index-item depth-1 d-flex align-items-center bg-transparent',
            `story-nav-${content.typeId.replace(/\..*/, '')}`
          )}
        >
          {Icon ? (
            <Icon
              className="text-muted me-1 flex-shrink-0"
              title={storyTypeName(polyglot, content.typeId)}
            />
          ) : null}
          <div className="text-truncate flex-shrink-1">
            {questions ? `Question ${index + 1} – ${content.data.title}` : content.data.title}
          </div>
          {restored && (
            <Badge
              color="warning"
              className="ms-2 text-dark"
            >
              Restored
            </Badge>
          )}
        </div>
      ) : (
        <Link
          to={`/branch/${branchId}/story/${content.name}?contextPath=${contextPath}${commitQuery}`}
          className={classNames(
            'story-index-item depth-1 d-flex align-items-center text-decoration-none',
            `story-nav-${content.typeId.replace(/\..*/, '')}`,
            canEdit && 'edit-mode',
            { created, deleted }
          )}
        >
          {Icon ? (
            <Icon
              className="content-type-icon me-1 flex-shrink-0"
              title={storyTypeName(polyglot, content.typeId)}
            />
          ) : null}
          <span
            className={classNames(
              'flex-shrink-1 text-truncate item-title',
              content.data.archived && 'archived-asset'
            )}
          >
            <span className="hover-underline">
              {questions ? (
                <>
                  {`Question ${index + 1} – `}
                  <span className="unhover-muted">{content.data.title || 'Untitled'}</span>
                </>
              ) : (
                content.data.title
              )}
            </span>
          </span>
          <Stornado name={content.name} />
          {accessRight && (
            <RiKeyLine
              className={classNames(
                'ms-2 access-restricted',
                'access-' + accessRightsMap[accessRight].toLowerCase()
              )}
              style={{ verticalAlign: '-.125rem' }}
              title={polyglot.t(accessRightI18n(accessRight))}
            />
          )}
          {edgeData.dueDateGate?.dueDateDayOffset != null && (
            <BsCalendarX
              className="ms-2 text-danger"
              title={`Due in ${edgeData.dueDateGate.dueDateDayOffset} Days`}
              size=".85rem"
            />
          )}
          {!!feedbackCount && contentAccess.ViewFeedback && (
            <span
              className="material-icons md-18 ms-2 has-feedback flex-shrink-0"
              title={plural(feedbackCount, 'feedback')}
            >
              chat_bubble_outline
            </span>
          )}
          <ContentStatusPill name={content.name} />
          {isMostRecent && (
            <IoBookmarkOutline
              className="most-recent ms-2 text-success flex-shrink-0"
              title={polyglot.t('MOST_RECENTLY_VISITED')}
              size=".85rem"
            />
          )}
          <div className="flex-grow-1" />
          {canEdit && (
            <div
              className="flex-shrink-0 flex-grow-0 controls ms-2"
              onClick={preventDefaults}
            >
              <Button
                size="sm"
                color="primary"
                outline
                className="border-0 d-inline-flex p-2 copy-btn"
                onClick={copyAsset}
                disabled={deleted}
                title="Copy"
                data-node-name={name}
              >
                <IoCopyOutline />
              </Button>
              <Button
                size="sm"
                color="primary"
                outline
                className="border-0 d-inline-flex p-2 cut-btn"
                onClick={cutAsset}
                disabled={deleted}
                title="Cut"
                data-node-name={name}
              >
                <IoCutOutline />
              </Button>
              <Button
                size="sm"
                color="danger"
                outline
                className="border-0 d-inline-flex p-2 delete-btn"
                onClick={removeAsset}
                disabled={deleted}
                title="Remove"
                data-node-name={name}
              >
                <IoTrashOutline />
              </Button>
            </div>
          )}
        </Link>
      )}
    </>
  );
};
