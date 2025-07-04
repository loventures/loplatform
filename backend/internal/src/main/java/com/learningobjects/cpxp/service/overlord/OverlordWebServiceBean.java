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

package com.learningobjects.cpxp.service.overlord;

import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.domain.DomainFacade;
import com.learningobjects.cpxp.service.domain.DomainState;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.upgrade.UpgradeService;
import com.learningobjects.cpxp.service.user.UserConstants;
import com.learningobjects.cpxp.util.Operation;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OverlordWebServiceBean extends BasicServiceBean implements OverlordWebService {
    private static final Logger logger = Logger.getLogger(OverlordWebServiceBean.class.getName());

    @Inject
    private FacadeService _facadeService;

    @Inject
    private UpgradeService _upgradeService;

    @Inject
    private AttachmentService _attachmentService;

    public Long findOverlordDomainId() {

        Item item = findOverlordDomainItem();

        return getId(item);
    }

    public DomainFacade findOverlordDomain() {

        DomainFacade facade = _facadeService.getFacade(findOverlordDomainItem(), DomainFacade.class);

        return facade;
    }

    private Item findOverlordDomainItem() {
        QueryBuilder qb = querySystem(DomainConstants.ITEM_TYPE_DOMAIN);
        qb.setCacheQuery(true);
        qb.setCacheNothing(false);
        qb.addCondition(DataTypes.DATA_TYPE_TYPE, "eq", DomainConstants.DOMAIN_TYPE_OVERLORD);
        return com.google.common.collect.Iterables.getFirst(qb.getItems(), null); //Default = null
    }

    public List<DomainFacade> getAllDomains() {

        QueryBuilder qb = querySystem(DomainConstants.ITEM_TYPE_DOMAIN);
        qb.addCondition(DataTypes.DATA_TYPE_TYPE, "ne", DomainConstants.DOMAIN_TYPE_OVERLORD);
        qb.addCondition(DataTypes.DATA_TYPE_TYPE, "ne", DomainConstants.DOMAIN_TYPE_STOCK);
        qb.addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE, "ne", DomainState.Deleted);
        qb.setOrder(DataTypes.DATA_TYPE_NAME, Function.LOWER, Direction.ASC);

        List<DomainFacade> domainFacades = new ArrayList<DomainFacade>();
        for (Item domain: qb.getItems()) {
            DomainFacade facade = _facadeService.getFacade(domain, DomainFacade.class);
            domainFacades.add(facade);
        }

        return domainFacades;
    }

    public Long getRootUserId(Long domainId) {

        Item rootUser = getDomainItemById(domainId, UserConstants.ID_USER_ROOT);

        return rootUser.getId();
    }

    public Long getAnonymousUserId(Long domainId) {

        Item anonymousUser = getDomainItemById(domainId, UserConstants.ID_USER_ANONYMOUS);

        return anonymousUser.getId();
    }

    public String getAdministrationUrl(Long domainId) {

        //Item adminPortal = getDomainItemById(domainId, DomainConstants.FOLDER_ID_ADMIN);
        //String url = DataTransfer.getStringData(adminPortal, DataTypes.DATA_TYPE_URL);

        return "/Administration";
    }

    @Override
    public <T> T asOverlord(Operation<T> operation) {
        return Operations.asDomain(findOverlordDomainId(), operation);
    }
}
