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

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.trash.TrashService;
import com.learningobjects.cpxp.util.GuidUtil;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import jakarta.persistence.Query;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.learningobjects.cpxp.service.user.UserConstants.*;
/**
 * User web service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class UserWebServiceBean extends BasicServiceBean implements UserWebService {

    /** The attachment service */
    @Inject
    private AttachmentService _attachmentService;

    /** The attachment web service. */
    @Inject
    private AttachmentWebService _attachmentWebService;

    /** The facade service. */
    @Inject
    private FacadeService _facadeService;

    /** The trash service. */
    @Inject
    private TrashService _trashService;

    /** The user service. */
    @Inject
    private UserService _userService;

    @Override
    public UserFacade addUser(Long parentId) {
        /*
        long limit = NumberUtils.longValue(Current.getDomainDTO().getUserLimit());

        if (limit > 0) {
            long userCount = countAllUsers(parentId);
            if (userCount >= limit) {
                throw new DomainLimitException(DomainLimitException.Limit.userLimit, userCount, limit);
            }
        }
        */

        UserFacade user = _facadeService.addFacade(parentId, UserFacade.class);

        user.setCreateTime(Current.getTime());
        user.bindUrl(GuidUtil.guid());
        user.setUserType(UserType.Standard);
        user.setUserState(UserState.Active);
        user.setDisabled(false);
        user.setInDirectory(false);

        return user;
    }

    public UserDTO getUserDTO(Long id) {
        UserFacade user = getUser(id);
        return (user == null) ? null : UserDTO.apply(user);
    }

    public UserFacade getUser(Long id) {

        Item item = _userService.get(id);
        UserFacade user = _facadeService.getFacade(item, UserFacade.class);

        return user;
    }

    public UserFacade getUserByUserName(String userName) {

        Item parent = _userService.getUserFolder();
        Item item = _userService.getByUserName(parent, userName);
        UserFacade user = _facadeService.getFacade(item, UserFacade.class);

        return user;
    }

    public void remove(Long id) {

        Item user = _userService.get(id);
        String trashId = _trashService.trash(id);

        Query query = createQuery("UPDATE ConnectionFinder SET del = :trashId WHERE user = :user");
        query.setParameter("trashId", trashId);
        query.setParameter("user", user.getFinder());
        query.executeUpdate();

    }

    public List<UserFacade> getAll(Long parentId) {

        Item parent = _itemService.get(parentId);
        List<UserFacade> users = new ArrayList<UserFacade>();
        for (Item user: findByParentAndType(parent, ITEM_TYPE_USER)) {
            UserFacade facade = _facadeService.getFacade(user, UserFacade.class);
            users.add(facade);
        }

        return users;
    }

    public Long getAnonymousUser() {

        Item user = getDomainItemById(ID_USER_ANONYMOUS);

        return getId(user);
    }

    public Long getRootUser() {

        Item user = getDomainItemById(ID_USER_ROOT);

        return getId(user);
    }

    public Long getRootUser(Long domainId) {

        Item user = getDomainItemById(domainId, ID_USER_ROOT);

        return getId(user);
    }

    public void setImage(Long userId, String fileName, Long width, Long height, File file, String thumbnail) {

        Item user = _userService.get(userId);
        Item image = _attachmentService.setImageData(user, DATA_TYPE_IMAGE, fileName, width, height, null, file);

        if (file != null) {
            flush(image);
            _attachmentWebService.setThumbnailGeometry(image.getId(), thumbnail);
        }
        // Force a version change. If you remove and add an image your version will return to 0 and cache
        // busting will fail. If I assigned a random id, could be better.
    }

    public Long getUserFolder() {

        return getUserFolder(Current.getDomain());
    }

    public Long getUserFolder(Long domainId) {

        Item usersFolder = _userService.getUserFolder(domainId);

        return getId(usersFolder);
    }


}
