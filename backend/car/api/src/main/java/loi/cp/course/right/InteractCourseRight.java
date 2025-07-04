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

package loi.cp.course.right;

import loi.cp.right.RightBinding;

/**
 * Right declaring that a user is allowed to read AND interact with content. Can submit quizzes, posts, etc.
 * This is strictly more expansive than ReadCourseRight.
 * ... and yet they have no inheritance relationship. ಠ_ಠ
 *
 * Note well that just because your role has this right does not mean _you_ have
 * this right, because by the awesome power of {@link loi.cp.right.RoleRightsProvider}
 * is this right removed from students in a course that has ended.
 */
@RightBinding(name = "Interact Course Access", description = "Access to interact with content within a course")
public class InteractCourseRight extends LearnCourseRight {
}
