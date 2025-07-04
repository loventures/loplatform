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

import { useContentResource } from '../../resources/ContentsResource';
import { useContentGatingInfoResource } from '../../resources/GatingInformationResource';
import { useCourseSelector } from '../../loRedux';
import ContentAvailabilityMessage from '../../contentPlayerComponents/parts/ContentAvailabilityMessage';
import { useTranslation } from '../../i18n/translationContext';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React, { SyntheticEvent, useState } from 'react';
import { Tooltip } from 'reactstrap';

const useGatingTooltip = (contentId: string, target?: string) => {
  const currentUser = useCourseSelector(selectCurrentUser);
  const { isInstructor, id: userId } = currentUser;
  const content = useContentResource(contentId, userId);
  const gatingInfo = useContentGatingInfoResource(contentId, userId);
  const { isLocked, isGated, selfGated } = gatingInfo;
  const locked = isLocked && !isInstructor;
  const [showTooltip, setShowTooltip] = useState(false);
  // don't overload instructors with lock icons on all the items within a module.
  const showLockIcon = isLocked || (isGated && isInstructor && selfGated);
  const showKeyIcon = !!content?.accessControlled; // is never gated too
  const translate = useTranslation();

  const navTargetId = target ?? `tooltip-target-${contentId}`;

  const displayTooltip = () => setShowTooltip(true);
  const hideTooltip = () => setShowTooltip(false);

  const GatingTooltip = () => (
    <Tooltip
      isOpen={showTooltip}
      target={navTargetId}
      autohide={false}
      className="tooltip-info"
      placement="right-end"
      modifiers={[{ name: 'preventOverflow', options: { mainAxis: false } } as any]}
    >
      <div role="tooltip">
        {showKeyIcon ? (
          <div className="px-3 py-2">{translate('ACCESS_IS_RESTRICTED')}</div>
        ) : (
          <ContentAvailabilityMessage
            content={{ name: content.name, availability: gatingInfo }}
            viewingAs={currentUser}
          />
        )}
      </div>
    </Tooltip>
  );

  return {
    showLockIcon,
    showKeyIcon,
    locked,
    GatingTooltip,
    linkProps: {
      id: navTargetId,
      onMouseEnter: isInstructor && !showKeyIcon ? undefined : displayTooltip,
      onFocus: isInstructor && !showKeyIcon ? undefined : displayTooltip,
      onMouseLeave: isInstructor && !showKeyIcon ? undefined : hideTooltip,
      onBlur: isInstructor && !showKeyIcon ? undefined : hideTooltip,
      disabled: locked,
      onClick: locked
        ? (evt: SyntheticEvent) => {
            evt.preventDefault();
            evt.stopPropagation();
          }
        : undefined,
    },
    iconProps: {
      locked: showLockIcon,
      enter: isInstructor ? displayTooltip : undefined,
      leave: isInstructor ? hideTooltip : undefined,
    },
    pointerEvents: showKeyIcon ? 'none' : undefined, // help the tooltip stay open
  } as const;
};

export default useGatingTooltip;
