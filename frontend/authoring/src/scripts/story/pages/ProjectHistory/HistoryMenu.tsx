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

import React, { useCallback } from 'react';
import { GiLeafSkeleton } from 'react-icons/gi';
import { IoMenuOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { useDcmSelector } from '../../../hooks';
import { DropdownAItem } from '../../components/DropdownAItem';
import { useProjectAccess } from '../../hooks';
import { testSectionAction } from '../../narrativeActions';

export const HistoryMenu: React.FC<{
  published: boolean;
}> = ({ published }) => {
  const dispatch = useDispatch();
  const project = useDcmSelector(state => state.layout.project);
  const rights = useDcmSelector(state => state.user.rights);
  const canPublish = rights?.includes('loi.authoring.security.right$PublishOfferingRight');

  const projectAccess = useProjectAccess();
  const onTestSection = useCallback(
    (e: React.MouseEvent) => dispatch(testSectionAction(e.metaKey || e.ctrlKey)),
    []
  );

  return canPublish ? (
    <UncontrolledDropdown className="d-inline-block">
      <DropdownToggle
        id="history-menu-toggle"
        color="primary"
        outline
        caret
        className="border-0 asset-settings unhover-muted hover-white"
      >
        <IoMenuOutline size="1.75rem" />
      </DropdownToggle>
      <DropdownMenu
        right
        id="history-menu"
      >
        <DropdownAItem
          id="offering-button"
          disabled={!published}
          href={`/Administration/CourseOfferings?project=${project.id}`}
        >
          Course Offering
        </DropdownAItem>
        <DropdownItem
          disabled={!projectAccess.TestSections}
          onClick={onTestSection}
        >
          New Test Section
        </DropdownItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  ) : (
    <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
      <GiLeafSkeleton size="1.75rem" />
    </div>
  );
};
