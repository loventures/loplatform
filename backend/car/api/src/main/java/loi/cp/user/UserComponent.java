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

package loi.cp.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.Url;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.validator.Validated;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.user.*;
import com.learningobjects.cpxp.util.GuidUtil;
import com.learningobjects.cpxp.validation.groups.Writable;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.web.DeletableEntity;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableId;
import com.learningobjects.de.web.QueryableProperties;
import loi.cp.attachment.AttachmentComponent;
import loi.cp.enrollment.MembershipComponent;
import loi.cp.integration.IntegrationRootOwner;

import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Validated(by = UserComponentValidator.class)
@Schema("user")
@ItemMapping(value = UserConstants.ITEM_TYPE_USER, singleton = true)
@QueryableProperties({
        @Queryable(name = "fullName", handler = UserFullNameHandler.class),
        @Queryable(name = "domainRole", handler = DomainRoleHandler.class, traits = Queryable.Trait.NOT_SORTABLE),
        @Queryable(name = "uniqueId", handler = UniqueIdHandler.class, traits = {Queryable.Trait.NOT_SORTABLE, Queryable.Trait.CASE_INSENSITIVE}),
        @Queryable(name = "accessTime", handler = AccessTimeHandler.class, traits = {Queryable.Trait.NOT_FILTERABLE})
})
public interface UserComponent
        extends ComponentInterface, IntegrationRootOwner, QueryableId,
        Url, DeletableEntity {

    @JsonView(Default.class)
    String getHandle();

    @JsonView(Default.class)
    UserState getUserState();

    public void transition(UserState state);

    @JsonProperty("user_type")
    @JsonView(Default.class)
    UserType getUserType();

    @NotNull
    @JsonProperty
    @Queryable(dataType = UserConstants.DATA_TYPE_USER_NAME,
            traits = Queryable.Trait.CASE_INSENSITIVE)
    String getUserName();

    void setUserName(String userName);

    @JsonProperty
    @Queryable(dataType = UserConstants.DATA_TYPE_EXTERNAL_ID, traits = Queryable.Trait.CASE_INSENSITIVE)
    Optional<String> getExternalId();

    void setExternalId(Optional<String> externalId);

    @JsonProperty
    @Queryable(dataType = UserConstants.DATA_TYPE_GIVEN_NAME, traits = Queryable.Trait.CASE_INSENSITIVE)
    String getGivenName();

    void setGivenName(String givenName);

    @JsonProperty
    @Queryable(dataType = UserConstants.DATA_TYPE_MIDDLE_NAME, traits = Queryable.Trait.CASE_INSENSITIVE)
    String getMiddleName();

    void setMiddleName(String middleName);

    @JsonProperty
    @Queryable(dataType = UserConstants.DATA_TYPE_FAMILY_NAME, traits = Queryable.Trait.CASE_INSENSITIVE)
    String getFamilyName();

    void setFamilyName(String familyName);

    @JsonView(Default.class)
    String getFullName();

    // @AlphaSpace
    // @Size(min=1, max=50)
    @JsonProperty
    String getTitle();

    void setTitle(String title);

    @JsonProperty
    @Queryable(dataType = UserConstants.DATA_TYPE_EMAIL_ADDRESS, traits = Queryable.Trait.CASE_INSENSITIVE)
    String getEmailAddress();

    void setEmailAddress(String emailAddress);

    // DON'T add these methods here...
    // instead use getUserState and transition
    // leaving these methods commented out as a warning
    // Boolean getDisabled();
    // void setDisabled(Boolean disabled);

    @JsonView(Writable.class)
    String getPassword();

    void setPassword(String password);

    @JsonView(Default.class)
    String getImageUrl();

    @JsonProperty("subtenant_id")
    @Queryable(dataType = UserConstants.DATA_TYPE_USER_SUBTENANT)
    Long getSubtenantId();

    void setSubtenant(Long subtenantId);

    @RequestMapping(path = "image", method = Method.GET)
    @Secured(byOwner = true)
    AttachmentComponent getImage();

    @RequestMapping(path = "image", method = Method.POST)
    @Secured(byOwner = true)
    void uploadImage(@QueryParam("image") UploadInfo upload);

    @RequestMapping(path = "thumbnail", method = Method.GET)
    @Secured(byOwner = true)
        // this is called not called getThumbnail because getXxx(param) confounds Mr Bean or one of his friends
    AttachmentComponent thumbnail(@MatrixParam String size);

    /**
     * Gets the domain role display names for the given user
     *
     * @return
     */
    @RequestMapping(path = "roles", method = Method.GET)
    List<String> getDomainRoleDisplayNames();

    /**
     * Gets the domain roles for the given user
     *
     * @return
     */
    List<RoleFacade> getDomainRoles();

    /**
     * Gets the user's domain-level rights
     *
     * @return
     */
    @RequestMapping(path = "rights", method = Method.GET)
    List<String> getDomainRights();

    //    @Secured(byOwner = true) what about admins
    @RequestMapping(path = "membership", method = Method.GET)
    public MembershipComponent getMembership();

    @RequestMapping(path = "preferences", method = Method.GET)
    UserPreferences getPreferences();

    @RequestMapping(path = "preferences", method = Method.PUT)
    UserPreferences updatePreferences(@RequestBody JsonNode prefs);

    @RequestMapping(path = "times", method = Method.GET)
    Times getTimes();

    UserDTO toDTO();

    /* UserId contract. */
    @JsonIgnore
    default long id() {
        return getId();
    }

    @JsonIgnore
    default UserId userId() { return new UserId(getId()); }

    class Init {
        public Optional<String> externalId = Optional.empty();
        public String userName;
        public String title;
        public String givenName;
        public String middleName;
        public String familyName;
        public String emailAddress;
        public String password;
        public boolean emailPassword;
        public UserState state;
        public String[] roles = {};
        public UniqueId[] uniqueIds = {};
        public UserType userType;
        public Long subtenantId;
        public String url = GuidUtil.guid();
    }

    class UniqueId {
        public Long integrationId;
        public Long systemId;
        public String uniqueId;
    }

    class Times {
        public Date createTime;
        public Date accessTime;
        public Date loginTime;
    }
}
