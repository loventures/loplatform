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

package com.learningobjects.cpxp.service.relationship;

import javax.annotation.Nonnull;
import javax.ejb.Local;
import java.util.List;
import java.util.Set;


/**
 * The relationship web service.
 */
@Local
public interface RelationshipWebService {
    // Role

    public RoleFacade getRole(Long id);

    public RoleFacade getRoleByRoleId(Long parentId, String roleId);

    @Nonnull
    public RoleFacade addRole(Long parentIdd);

    public void removeRole(Long id);

    public Set<RoleFacade> getSystemRoles();

    /**
     * @param itemId
     *
     * @return the roles defined locally on the given item
     */
    public List<RoleFacade> getLocalRoles(Long itemId);

    public void addSupportedRole(Long itemId, Long id);


    /**
     * @param itemId
     * @return the roles supported on the given item
     */
    public List<RoleFacade> getSupportedRoles(Long itemId);

    public Long getRoleFolder();
}
