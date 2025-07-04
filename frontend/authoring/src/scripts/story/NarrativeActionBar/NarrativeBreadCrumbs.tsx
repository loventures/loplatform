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

import React, { useEffect } from 'react';
import { IoSchoolOutline } from 'react-icons/io5';
import { Link } from 'react-router-dom';

import { trackNarrativeNavHandler } from '../../analytics/AnalyticsEvents';
import { useEpicScreen } from '../../feedback/feedback';
import {
  useCurrentAssetName,
  useCurrentContextPath,
  useEditedAssetTitle,
  useEditedAssetTypeId,
} from '../../graphEdit';
import { useDcmSelector, useHomeNodeName } from '../../hooks';
import { setMostRecent } from '../../hooks/useMostRecent';
import { getIcon } from '../AddAsset';
import { contextPathQuery, truncateAssetTitle } from '../story';
import { useRevisionCommit } from '../storyHooks';
import { LastCrumb } from './LastCrumb';
import { ParentCrumb } from './ParentCrumb';
import { SimpleLastCrumb } from './SimpleLastCrumb';
import { SimpleParentCrumb } from './SimpleParentCrumb';

export const NarrativeBreadCrumbs: React.FC = () => {
  const name = useCurrentAssetName();
  const contextPath = useCurrentContextPath();
  const contextNames = contextPath?.split('.') ?? [];

  const homeName = useHomeNodeName();
  const homeTitle = useEditedAssetTitle(homeName);
  const epicScreen = useEpicScreen();

  const simpleCrumbs = useDcmSelector(state => !state.projectStructure.hidden);

  const parentName = contextNames[contextNames.length - 1];

  const grandparentName = contextNames[contextNames.length - 2];
  const grandparentTitle = useEditedAssetTitle(grandparentName);
  const grandparentTypeId = useEditedAssetTypeId(grandparentName);

  const greatGrandparentName = contextNames[contextNames.length - 3];
  const greatGrandparentTitle = useEditedAssetTitle(greatGrandparentName);
  const greatGrandparentTypeId = useEditedAssetTypeId(greatGrandparentName);

  const commit = useRevisionCommit();

  const GrandparentIcon = getIcon(grandparentTypeId);
  const GreatGrandparentIcon = getIcon(greatGrandparentTypeId);

  useEffect(() => {
    if (name && parentName) setMostRecent(parentName, name);
  }, [name, parentName]);

  const elision = contextNames.length > 4;

  return (
    <>
      {((name !== homeName && contextNames[0] !== homeName) || contextNames.length > 3) && (
        <>
          <Link
            to={homeName + contextPathQuery('', commit)}
            onClick={trackNarrativeNavHandler('Course')}
            className="minw-0 text-truncate course-crumb"
            title={homeTitle}
          >
            <IoSchoolOutline />
          </Link>
          <span
            className="text-muted ms-2 me-2"
            style={elision ? { letterSpacing: '-1.5px' } : undefined}
          >
            {elision ? '/···/' : '/'}
          </span>
        </>
      )}
      {greatGrandparentName ? (
        <>
          <Link
            to={
              greatGrandparentName + contextPathQuery(contextNames.slice(0, -3).join('.'), commit)
            }
            onClick={trackNarrativeNavHandler('Great Grandparent')}
            className="minw-0 text-truncate great-grandparent-crumb"
            title={greatGrandparentTitle}
          >
            <GreatGrandparentIcon />
          </Link>
          <span className="text-muted ms-2 me-2">/</span>
        </>
      ) : null}
      {grandparentName ? (
        <>
          <Link
            to={grandparentName + contextPathQuery(contextNames.slice(0, -2).join('.'), commit)}
            onClick={trackNarrativeNavHandler('Grandparent')}
            className="minw-0 text-truncate grandparent-crumb"
            title={grandparentTitle}
          >
            {epicScreen ? (
              truncateAssetTitle(grandparentTitle, grandparentTypeId)
            ) : (
              <GrandparentIcon />
            )}
          </Link>
          <span className="text-muted ms-2 me-2">/</span>
          {simpleCrumbs ? (
            <SimpleParentCrumb
              parentName={parentName}
              contextPath={contextNames.slice(0, -1).join('.')}
            />
          ) : (
            <ParentCrumb
              grandparentName={grandparentName}
              parentName={parentName}
              contextPath={contextNames.slice(0, -1).join('.')}
            />
          )}
        </>
      ) : parentName ? (
        <SimpleParentCrumb
          parentName={parentName}
          contextPath={contextNames.slice(0, -1).join('.')}
        />
      ) : null}
      {simpleCrumbs ? (
        <SimpleLastCrumb
          name={name}
          contextPath={contextPath}
        />
      ) : (
        <LastCrumb
          parentName={parentName}
          name={name}
          contextPath={contextPath}
        />
      )}
    </>
  );
};
