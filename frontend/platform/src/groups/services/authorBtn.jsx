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

import React from 'react';

import LeafSvg from '../../imgs/leaf-white.svg';

/*
 * Button from admin to authoring in a specific version and project.
 */
const getAuthorBtn = (selectedRow, rights) => {
  const projectLink =
    selectedRow &&
    selectedRow.project_id &&
    selectedRow.version_id &&
    selectedRow.project_homeNodeName &&
    `/Authoring/branch/${selectedRow.version_id}/launch/${selectedRow.project_homeNodeName}`;
  const authorBtnConfig = {
    name: 'author',
    iconName: (
      <img
        src={LeafSvg}
        alt=""
        style={{ verticalAlign: 'text-bottom', width: '14px', height: '14px', marginBottom: '2px' }}
      />
    ),
    href: projectLink ? projectLink : null,
    disabled: !projectLink,
  };
  const isContentAuthor = rights.includes('loi.cp.admin.right.ContentAuthorRight');
  return isContentAuthor ? [authorBtnConfig] : [];
};

export default getAuthorBtn;
