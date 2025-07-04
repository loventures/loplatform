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

import style from 'react-treebeard/dist/themes/default';

// Setup tree style. This is weird.
style.tree.base.backgroundColor = '#fff';
style.tree.node.header.base.color = 'black';
style.tree.node.toggle.base.marginLeft = 0;
style.tree.node.toggle.base.transformOrigin = '7px 12px';
style.tree.node.toggle.wrapper.position = undefined;
style.tree.node.toggle.wrapper.margin = undefined;
style.tree.node.toggle.arrow.fill = 'black';
style.tree.node.activeLink.background = '#3875d7';

export { style };
