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

package com.learningobjects.cpxp.service.group;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.trash.TrashService;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import jakarta.persistence.Query;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Group web service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class GroupWebServiceBean extends BasicServiceBean implements GroupWebService {
    private static final Logger logger = Logger.getLogger(GroupWebServiceBean.class.getName());
    /** The user service. */
    @Inject
    private GroupService _groupService;

    @Inject
    private FacadeService _facadeService;

    /** the attachment service */
    @Inject
    private AttachmentService _attachmentService;

    @Inject
    private AttachmentWebService _attachmentWebService;

    @Inject
    private TrashService _trashService;

    public GroupFacade addGroup(Long parentId) {
        /*
        DomainDTO domain = Current.getDomainDTO();
        if (domain.getGroupLimit() != null) {
            long limit = domain.getGroupLimit().longValue();
            long groups = countAllGroups(parentId);
            if (groups >= limit) {
                throw new DomainLimitException(DomainLimitException.Limit.groupLimit, groups, limit);
            }
        }
        */

        GroupFacade group = _facadeService.addFacade(parentId, GroupFacade.class);

        group.setCreateTime(new Date());

        return group;
    }

    public GroupFacade getGroup(Long id) {

        Item item = _groupService.getGroup(id);
        GroupFacade group = _facadeService.getFacade(item, GroupFacade.class);

        return group;
    }

    public GroupFacade getRawGroup(Long id) {

        Item item = _groupService.getGroup(id);
        GroupFacade group = _facadeService.getFacade(item, GroupFacade.class);

        return group;
    }

    public void removeGroup(Long id) {

//TODO:        _accessControlWebService.getAccessControlDecision(id).checkPermission(PERMISSION_DELETE_GROUP);

        Item group = _groupService.getGroup(id);
        String trashId = _trashService.trash(id);

        Query query = createQuery("UPDATE EnrollmentFinder SET del = :trashId WHERE group = :group");
        query.setParameter("trashId", trashId);
        query.setParameter("group", group);
        query.executeUpdate();

    }

    public GroupFacade getGroupByGroupId(Long parentId, String groupId) {
        Item parent = _itemService.get(parentId);
        Item group = _groupService.getGroupByGroupId(parent, groupId);
        return (group == null) ? null : getGroup(group.getId());
    }

    public List<GroupFacade> getAllGroups(Long parentId, Integer start, Integer limit, String dataType, Boolean ascending) {

        Item parent = _itemService.get(parentId);
        List<GroupFacade> groups = new ArrayList<GroupFacade>();
        for (Item group: _groupService.getAllGroups(parent, start, limit,dataType, ascending)) {
            GroupFacade facade = getGroup(group.getId());
            if (facade != null) {
                groups.add(facade);
            }
        }

        return groups;
    }

    public List<GroupFacade> getGroups(Long parentId) {

        Item parent = _itemService.get(parentId);
        List<GroupFacade> groups = new ArrayList<GroupFacade>();
        QueryBuilder qb = queryParent(parent,GroupConstants.ITEM_TYPE_GROUP);
        qb.setOrder(DataTypes.DATA_TYPE_NAME, Direction.ASC);
        for (Item group: qb.getItems()) {
            groups.add(_facadeService.getFacade(group, GroupFacade.class));
        }

        return groups;
    }

    public void setImage(Long groupId, String fileName, Long width, Long height, File file, String thumbnail) {

        Item group = _itemService.get(groupId);
        Item image = _attachmentService.setImageData(group,GroupConstants.DATA_TYPE_IMAGE, fileName, width, height, null, file);

        if (file != null) {
            flush(image);
            _attachmentWebService.setThumbnailGeometry(image.getId(), thumbnail);
        }

    }

    public Long getGroupFolder(Long domainId) {

        Item groupFolder = getDomainItemById(domainId, GroupConstants.ID_FOLDER_GROUPS);

        return getId(groupFolder);
    }

 }
