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

package loi.cp.accountRequest;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Messages;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.component.template.LohtmlTemplate;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException;
import com.learningobjects.cpxp.filter.CurrentFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.email.EmailService;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.exception.AccessForbiddenException;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.user.UserConstants;
import com.learningobjects.cpxp.service.user.UserState;
import com.learningobjects.cpxp.service.user.UserWebService;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.admin.right.HostingAdminRight;
import loi.cp.config.ConfigurationService;
import loi.cp.domain.DomainRootComponent;
import loi.cp.password.*;
import loi.cp.right.Right;
import loi.cp.right.RightService;
import loi.cp.role.RoleComponent;
import loi.cp.user.UserComponent;
import loi.cp.user.UserParentFacade;
import scala.Function1;
import scaloi.GetOrCreate;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Messages({
        "$$email_submit_subject=${domain.name} - Account request submitted",
        "$$email_submit_body=${user.givenName} -\nYour account request was submitted. You will receieve an email when it has been approved.",

        "$$email_create_subject=${domain.name} - Account created",
        "$$email_create_body=${user.givenName} -\nYour account was created.",

        "$$email_request_subject=${domain.name} - Account request received: ${request.user.fullName}",
        "$$email_request_body=An account request was received:\n\n  Name: ${request.user.fullName}\n  Email Address: ${request.user.emailAddress}\n  Request Attributes: ${request.attributes}",

        "$$email_accept_subject=${domain.name} - Account request approved",
        "$$email_accept_body=${user.givenName} -\nYour account request was approved.",

        "$$email_reject_subject=${domain.name} - Account request denied",
        "$$email_reject_body=${user.givenName} -\nYour account request was denied."
    })
public class AccountRequestRoot extends AbstractComponent implements AccountRequestRootComponent {
    private static final Logger logger = Logger.getLogger(AccountRequestRoot.class.getName());

    @Inject
    private ConfigurationService _configurationService;

    @Inject
    private DomainRootComponent _domainRoot;

    @Inject
    private EmailService _emailService;

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    @Inject
    private FacadeService _facadeService;

    @Inject
    private PasswordRootApi _passwordRoot;

    @Inject
    private RightService _rightService;

    @Inject
    private UserPasswordService _userPasswordService;

    @Inject
    private UserWebService _userWebService;

    @Override
    public void requestAccount(AccountRequestComponent request, String redirect) {
        final AccountRequestSettings settings =
          _configurationService.getDomain(PasswordRootApiImpl.passwordSettings()).accountRequests();
        final Status status = settings.statusEnum();
        if (Status.Disabled.equals(status)) {
            throw new AccessForbiddenException("Account requests not enabled");
        }
        UserComponent user = request.getUser();
        if (StringUtils.isBlank(user.getUserName()) ||
            StringUtils.isBlank(user.getGivenName()) ||
            StringUtils.isBlank(user.getFamilyName()) ||
            (Boolean.TRUE.equals(settings.usernameIsEmailAddress()) && !user.getUserName().equals(user.getEmailAddress())) ||
            !_emailService.validateEmailAddress(user.getEmailAddress())) {
            throw new InvalidRequestException("Invalid parameters");
        }
        if (getFolder().findUserByUsername(user.getUserName()).isPresent()) { // prefer to fail early
            throw new AccountRequestException(AccountRequestException.Reason.DuplicateUser);
        }
        if (!Status.Verify.equals(status)) {
            final List<String> errors = _passwordRoot.getPasswordErrors(user, user.getPassword());
            if (!errors.isEmpty()) {
                throw new AccountRequestException(AccountRequestException.Reason.InvalidPassword, errors);
            }
        }
        UserComponent.Init init = new UserComponent.Init();
        init.userName = user.getUserName();
        init.title = user.getTitle();
        init.givenName = user.getGivenName();
        init.middleName = user.getMiddleName();
        init.familyName = user.getFamilyName();
        init.emailAddress = user.getEmailAddress();
        init.password = Status.Verify.equals(status) ? null : user.getPassword();
        init.state = Status.Manual.equals(status) ? UserState.Pending : UserState.Active;
        GetOrCreate<UserComponent> goc = getFolder().getOrCreateUserByUsername(user.getUserName(), init);
        if (goc.isGotten()) {
            throw new AccountRequestException(AccountRequestException.Reason.DuplicateUser);
        }

        UserComponent created = goc.result();
        created.asComponent(AccountRequestComponent.class).init(request);

        switch (status) {
            case Verify:
                final String redirectUrl = Optional.ofNullable(redirect).orElse("/#/resetPassword/");
                final Function1<String, String> urlGenerator =
                  token -> redirectUrl + token;
                ChangePasswordReceipt receipt =
                  _userPasswordService.resetPassword(created, true, urlGenerator);
                receipt.emailError().foreach(error -> {
                    throw new RuntimeException("Error sending email to: " + user.getEmailAddress() + ": " + error);
                });
                break;

            case Automatic:
                sendEmail(created, "create", created.asComponent(AccountRequestComponent.class));
                CurrentFilter.login(BaseWebContext.getContext().getRequest(),
                    BaseWebContext.getContext().getResponse(),
                    _userWebService.getUserDTO(created.getId()), false);
                break;

            case Manual:
                sendEmail(created, "submit", created.asComponent(AccountRequestComponent.class));
                for (String username : StringUtils.splitString(settings.notifyUsers())) {
                    notify(username, created);
                }
                break;
        }
    }

    private void notify(String username, UserComponent created) {
        Optional<UserComponent> admin = getFolder().findUserByUsername(username);
        if (!admin.isPresent()) {
            logger.log(Level.WARNING, "Unknown registration notification user: " + username);
            return;
        }
        sendEmail(admin.get(), "request", created.asComponent(AccountRequestComponent.class));
    }

    @Override
    public ApiQueryResults<AccountRequestComponent> getAccountRequests(ApiQuery query) {
        QueryBuilder qb = getFolder().queryUsers();
        qb.addCondition(UserConstants.DATA_TYPE_USER_STATE, "eq", UserState.Pending);
        return ApiQuerySupport.query(qb, query, AccountRequestComponent.class);
    }

    @Override
    public void acceptAccount(Long id, Boolean email, Long role) {
        UserComponent user = ComponentSupport.get(id, UserComponent.class);
        if ((user == null) || !UserState.Pending.equals(user.getUserState())) {
            throw new ResourceNotFoundException("Unknown account registration: " + id);
        } else if ((role != null) && !isValidRole(role)) {
            throw new AccessForbiddenException("Invalid role: " + role);
        }
        if (role != null) {
            _enrollmentWebService.setEnrollment(Current.getDomain(), role, id, "AcceptAccount");
        }
        user.transition(UserState.Active);
        getFolder().invalidate();
        if (Boolean.TRUE.equals(email)) {
            sendEmail(user, "accept", user.asComponent(AccountRequestComponent.class));
        }
    }

    private boolean isValidRole(Long role) {
        boolean okay = false;
        for (RoleComponent valid : getAccountRequestRoles()) {
            okay |= role.equals(valid.getId());
        }
        return okay;
    }

    @Override
    public void rejectAccount(Long id, Boolean email) {
        UserComponent user = ComponentSupport.get(id, UserComponent.class);
        if ((user == null) || !UserState.Pending.equals(user.getUserState())) {
            throw new ResourceNotFoundException("Unknown account registration: " + id);
        }
        user.transition(UserState.Suspended);
        getFolder().invalidate();
        if (Boolean.TRUE.equals(email)) {
            sendEmail(user, "reject", user.asComponent(AccountRequestComponent.class));
        }
    }

    @Override
    public List<RoleComponent> getAccountRequestRoles() {
        Set<Class<? extends Right>> adminRights = _rightService.getDescendants(HostingAdminRight.class);
        List<RoleComponent> roles = _domainRoot.getDomain().getSupportedRoles();
        roles.removeIf(role -> !Collections.disjoint(adminRights, _rightService.getRoleRights(role)));
        return roles;
    }

    private void sendEmail(UserComponent user, String type, AccountRequestComponent request) {
        try {
            String subject = ComponentUtils.getMessage("email_" + type + "_subject", getComponentDescriptor());
            String body = ComponentUtils.getMessage("email_" + type + "_body", getComponentDescriptor());
            JsonMap map = JsonMap.of("domain", Current.getDomainDTO())
                .add("user", user)
                .add("request", request);
            subject = LohtmlTemplate.expandString(subject, getComponentDescriptor(), map);
            body = LohtmlTemplate.expandString(body, getComponentDescriptor(), map);
            if (StringUtils.isEmpty(user.getEmailAddress())) {
                throw new Exception("No email address");
            }
            _emailService.sendTextEmail(null, null, user.getEmailAddress(), user.getFullName(), subject, body, false);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error sending email", ex);
            // TODOO: really just silently drop?
        }
    }

    @Override
    public Status getAccountRequestStatus() {
        return _configurationService.getDomain(PasswordRootApiImpl.passwordSettings()).accountRequests().statusEnum();
    }

    private UserParentFacade getFolder() {
        return _facadeService.getFacade(UserConstants.ID_FOLDER_USERS, UserParentFacade.class);
    }
}
