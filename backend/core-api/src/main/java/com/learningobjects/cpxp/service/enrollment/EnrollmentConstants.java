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

package com.learningobjects.cpxp.service.enrollment;

public interface EnrollmentConstants {
    // User deletion won't invalidate membership.. Would need triggers.
    String INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP = "membership-";
    String INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP = "membership-";

    String ITEM_TYPE_ENROLLMENT = "Enrollment";
    String DATA_TYPE_ENROLLMENT_CREATED_ON = "Enrollment.createdOn";
    String DATA_TYPE_ENROLLMENT_GROUP = "Enrollment.group";
    String DATA_TYPE_ENROLLMENT_ROLE = "Enrollment.role";
}
