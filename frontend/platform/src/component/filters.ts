/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

export interface TreeNode {
  name: string;
  children?: TreeNode[];
  toggled?: boolean;
  [key: string]: unknown;
}

export type Matcher = (filterText: string, node: TreeNode) => boolean;

// Helper functions for filtering
export const defaultMatcher: Matcher = (filterText, node) => {
  return node.name.toLowerCase().indexOf(filterText.toLowerCase()) !== -1;
};

export const findNode = (node: TreeNode, filter: string, matcher: Matcher): boolean => {
  return (
    matcher(filter, node) ||
    (!!node.children &&
      node.children.length > 0 &&
      !!node.children.find(child => findNode(child, filter, matcher)))
  );
};

export const filterTree = (
  node: TreeNode,
  filter: string,
  matcher: Matcher = defaultMatcher
): TreeNode => {
  // If im an exact match then all my children get to stay
  if (matcher(filter, node) || !node.children) {
    return node;
  }
  // If not then only keep the ones that match or have matching descendants
  const filtered = node.children
    .filter(child => findNode(child, filter, matcher))
    .map(child => filterTree(child, filter, matcher));
  return Object.assign({}, node, { children: filtered });
};

export const expandFilteredNodes = (
  node: TreeNode,
  filter: string,
  matcher: Matcher = defaultMatcher
): TreeNode => {
  let children = node.children;
  if (!children || children.length === 0) {
    return Object.assign({}, node, { toggled: false });
  }
  const childrenWithMatches = children.filter(child => findNode(child, filter, matcher));
  const shouldExpand = childrenWithMatches.length > 0;
  // If im going to expand, go through all the matches and see if thier children need to expand
  if (shouldExpand) {
    children = childrenWithMatches.map(child => {
      return expandFilteredNodes(child, filter, matcher);
    });
  }
  return Object.assign({}, node, {
    children: children,
    toggled: shouldExpand,
  });
};
