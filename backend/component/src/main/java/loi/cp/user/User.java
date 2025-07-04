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

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.attachment.ImageFacade;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.login.LoginWebService;
import com.learningobjects.cpxp.service.relationship.RelationshipWebService;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.subtenant.SubtenantFacade;
import com.learningobjects.cpxp.service.user.*;
import com.learningobjects.cpxp.util.FormattingUtils;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.attachment.AttachmentComponent;
import loi.cp.config.ConfigurationService;
import loi.cp.enrollment.MembershipComponent;
import loi.cp.password.PasswordRootApi;
import loi.cp.right.RightService;
import loi.cp.web.HandleService;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class User extends AbstractComponent implements UserComponent {

    @Inject
    private AttachmentWebService _attachmentWebService;

    @Inject
    private ConfigurationService _configurationService;

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    @Inject
    private FacadeService _facadeService;

    @Inject
    private HandleService _handleService;

    @Inject
    private LoginWebService _loginWebService;

    @Inject
    private RelationshipWebService _relationshipWebService;

    @Inject
    private RightService _rightService;

    @Inject
    private UserWebService _userWebService;

    @Instance
    private UserFacade _self;

    @PostCreate
    private void init(Init init) {

        _self.setCreateTime(Current.getTime());

        _self.setUserExternalId(init.externalId);
        _self.setUserName(init.userName);
        _self.setUserTitle(init.title);
        _self.setGivenName(init.givenName);
        _self.setMiddleName(init.middleName);
        _self.setFamilyName(init.familyName);
        _self.setEmailAddress(init.emailAddress);
        _self.updateFullName();
        _self.setInDirectory(false);
        if (init.url != null) {
            _self.bindUrl(init.url);
        }
        _self.setUserType((init.userType == null) ? UserType.Standard : init.userType);
        transition((init.state == null) ? UserState.Active : init.state);
        if (StringUtils.isNotEmpty(init.password)) {
            setPassword(init.password);
        }
        if (init.roles != null && init.roles.length > 0) {
            // TODO: omfg, just make that better api
            Set<Long> roles = new HashSet<>();
            Long folder = _relationshipWebService.getRoleFolder();
            for (String name : init.roles) {
                RoleFacade role = _relationshipWebService.getRoleByRoleId(folder, name);
                if (role == null) {
                    throw new ValidationException("role", name, "Unknown role: " + name);
                }
                roles.add(role.getId());
            }
            _enrollmentWebService.setEnrollments(Current.getDomain(),
                    roles, getId(), null, null, null, false);
        }

    }

    @Override
    public Long getId() {
        return _self.getId();
    }

    @Override
    public String getHandle() {
        return _handleService.maskId(_self.getId());
    }

    @Override
    public String getUrl() {
        return _self.getUrl();
    }

    @Override
    public String getTitle() {
        return _self.getUserTitle();
    }

    @Override
    public void setTitle(String title) {
        _self.setUserTitle(title);
    }

    @Override
    public String getUserName() {
        return _self.getUserName();
    }

    @Override
    public void setUserName(final String userName) {
        _self.setUserName(userName);
    }

    @Override
    public Optional<String> getExternalId() {
        return _self.getUserExternalId();
    }

    @Override
    public void setExternalId(Optional<String> externalId) {
        _self.setUserExternalId(externalId);
    }

    // a forwarding method defined to allow super.setGivenName(...) invocation by a subclass.
    @Override
    public void setGivenName(final String givenName) {
        _self.setGivenName(givenName);
        _self.updateFullName();
    }

    @Override
    public String getGivenName() {
        return _self.getGivenName();
    }

    // a forwarding method defined to allow super.setMiddleName(...) invocation by a subclass.
    @Override
    public void setMiddleName(final String middleName) {
        _self.setMiddleName(middleName);
        _self.updateFullName();
    }

    @Override
    public String getMiddleName() {
        return _self.getMiddleName();
    }

    // a forwarding method defined to allow super.setFamilyName(...) invocation by a subclass.
    @Override
    public void setFamilyName(final String familyName) {
        _self.setFamilyName(familyName);
        _self.updateFullName();
    }

    @Override
    public String getFamilyName() {
        return _self.getFamilyName();
    }

    // a forwarding method defined to allow super.setEmailAddress(...) invocation by a subclass.
    @Override
    public void setEmailAddress(final String emailAddress) {
        _self.setEmailAddress(emailAddress);
    }

    @Override
    public String getEmailAddress() {
        return _self.getEmailAddress();
    }

    @Override
    public String getFullName() {
        return FormattingUtils.userStr(_self);
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public void setPassword(final String password) {
        _loginWebService.setPassword(getId(), password);
        // Autoexpire if an administrator changes a password. This excludes the user changing their
        // own password, the user choosing their inital password (anonymous) or an external system
        // setting a password (to support integration tests...)
        // TODO: this logic is tortuous; the admin page should pass in a flag to autoexpire the set password
        boolean autoexpire = !Current.isAnonymous()
                && !getId().equals(Current.getUser())
                && !UserType.System.equals(Current.getUserDTO().getUserType());
        Date changed = autoexpire ? new Date(0) : Current.getTime();
        ComponentSupport.get(PasswordRootApi.class)
                .recordPasswordChange(this, password, changed);
    }

    @Override
    public String getImageUrl() {
        return _self.getImage() != null ? _self.getImage().getUrl() : null;
    }

    @Override
    public AttachmentComponent getImage() {
        return ComponentSupport.get(_self.getImage(), AttachmentComponent.class);
    }

    @Override
    public AttachmentComponent thumbnail(String size) {
        Optional<AttachmentComponent.ThumbnailSize> thumbnailSize = AttachmentComponent.ThumbnailSize.parse(size);
        if ((_self.getImage() == null) || !thumbnailSize.isPresent()) {
            return null;
        }
        Long img = _self.getImage().getId();
        Long thumb = _attachmentWebService.getScaledImage(img, String.valueOf(thumbnailSize.get().getSize()));
        return ComponentSupport.get(thumb, AttachmentComponent.class);
    }

    @Override
    public void uploadImage(UploadInfo upload) {
        if (upload == UploadInfo.REMOVE) {
            Optional.ofNullable(_self.getImage()).ifPresent(ImageFacade::delete);
        } else {
            _userWebService.setImage(_self.getId(), upload.getFileName(), upload.getWidth(), upload.getHeight(), upload.getFile(), null);
        }
    }

    @Override
    public UserState getUserState() {
        return _self.getUserState();
    }

    @Override
    public UserType getUserType() {
        return _self.getUserType();
    }

    @Override
    public Long getSubtenantId() {
        return _self.getSubtenant().map(SubtenantFacade::getId).orElse(null);
    }

    @Override
    public void setSubtenant(Long subtenant) {
        SubtenantFacade subtenantFacade = _facadeService.getFacade(subtenant, SubtenantFacade.class);
        _self.setSubtenant(Optional.ofNullable(subtenantFacade));
    }

    @Override
    public void transition(UserState state) {
        _self.setUserState(state);
        _self.setDisabled(state.getDisabled());
        _self.setInDirectory(state.getInDirectory());
    }

    @Override
    public void delete() {
        _self.delete();
    }

    @Override
    public Times getTimes() {
        var times = new Times();
        times.createTime = _self.getCreateTime();
        _self.getHistory().ifPresent(history -> {
            times.accessTime = history.getAccessTime();
            times.loginTime = history.getLoginTime();
        });
        return times;
    }

    @Override
    public UserDTO toDTO() {
        return UserDTO.apply(_self);
    }

    @Override
    public List<String> getDomainRoleDisplayNames() {
        return getDomainRoles().stream().map(FormattingUtils::roleStr).collect(Collectors.toList());
    }

    @Override
    public List<RoleFacade> getDomainRoles() {
        return _enrollmentWebService.getActiveUserRoles(_self.getId(), Current.getDomain());
    }

    @Override
    public List<String> getDomainRights() {
        return _rightService.getUserRights().stream().map(Class::getName).collect(Collectors.toList());
    }

    @Override
    public MembershipComponent getMembership() {
        return asComponent(MembershipComponent.class);
    }

    @Override
    public UserPreferences getPreferences() {
        return _configurationService.getItem(UserPreferences.instance(), _self);
    }

    @Override
    public UserPreferences updatePreferences(JsonNode prefs) {
        _configurationService.patchItem(UserPreferences.instance(), _self, prefs);
        return getPreferences();
    }
}
