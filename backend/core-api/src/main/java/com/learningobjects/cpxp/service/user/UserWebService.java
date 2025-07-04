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

package com.learningobjects.cpxp.service.user;

import java.io.File;
import java.util.List;
import javax.ejb.Local;

import com.learningobjects.cpxp.service.item.Item;

/**
 * User web service.
 */
@Local
public interface UserWebService {
    UserFacade addUser(Long parentId);

    UserDTO getUserDTO(Long id);

    UserFacade getUser(Long id);

    UserFacade getUserByUserName(String userName);

    void remove(Long id);

    List<UserFacade> getAll(Long parent);

    Long getAnonymousUser();

    Long getRootUser();

    Long getRootUser(Long domainId);

    void setImage(Long user, String fileName, Long width, Long height, File profileFile, String geometry);

    Long getUserFolder();

    Long getUserFolder(Long domainId);
}
