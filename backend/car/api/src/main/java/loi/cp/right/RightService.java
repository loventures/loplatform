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

package loi.cp.right;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.overlord.OverlordWebService;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.service.user.UserFacade;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.de.authorization.CollectedAuthorityManager;
import com.learningobjects.de.authorization.CollectedRights;
import com.learningobjects.de.authorization.SecurityContext;
import loi.cp.admin.right.AdminRight;
import loi.cp.apikey.ApiKeySystem;
import loi.cp.bootstrap.Bootstrap;
import loi.cp.role.RoleService;
import loi.cp.role.SupportedRoleFacade;
import loi.cp.user.UserComponent;
import org.apache.commons.lang3.tuple.Pair;
import scala.compat.java8.OptionConverters;
import scala.jdk.javaapi.CollectionConverters;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RightService extends AbstractComponent implements ApiRootComponent {
    private static final Logger logger = Logger.getLogger(RightService.class.getName());

    @Inject
    private CollectedAuthorityManager _collectedAuthorityManager;

    @Inject
    private FacadeService _facadeService;

    @Inject
    private ItemService _itemService;

    @Inject
    private OverlordWebService _overlordWebService;

    @Infer
    private RightCache _rightCache;

    @Inject
    private RoleService _roleService;

    @Inject
    @VisibleForTesting
    ComponentEnvironment _environment;

    /**
     * Get the current user's rights in the domain.
     */
    public Set<Class<? extends Right>> getUserRights() {
        return getUserRights(Current.getDomainDTO());
    }


    /**
     * Get the current user's rights in the given context, unless the current user is overlord, then this method
     * returns
     * all rights.
     */
    public Set<Class<? extends Right>> getUserRights(Id context) {
        return getUserRights(context, Current.getUserDTO());
    }

    /**
     * Designated getUserRights method. Get the given user's rights in the given context.
     */
    public Set<Class<? extends Right>> getUserRights(Id context, Id user) {
        final boolean userInDomain = Current.getUser().equals(user.getId()) && Current.getDomain().equals(context.getId());
        if (userInDomain && UserType.System.equals(Current.getUserDTO().getUserType())) {
            return getApiKeyUserRights();
        }

        scala.Option<Set<Class<? extends Right>>> cached =
          _rightCache.get(Pair.of(user.getId(), context.getId()));

        if (cached.isEmpty()) {
            Set<Class<? extends Right>> rights = new HashSet<>();

            if (userInDomain && Current.isRoot() && user.getId().equals(_overlordWebService.getRootUserId(context.getId())))  {
                // The internal root user used by things like appevents etc has no role, so no role-based rights
                return getDescendants(Right.class);
            } else {
                for (RoleFacade role : _roleService.getRolesForUser(context, user)) {
                    rights.addAll(getRoleRights(context, role));
                }

                // TODO: Add actual guest right (null role => guest)
                rights.addAll(getRoleRights(context, null));
            }

            // depend on all the supported roles so if they are changed this is evicted.
            List<? extends Id> supportedRoles = _roleService.getSupportedRoles(context);
            RightEntry cacheEntry = new RightEntry(rights, user, context, supportedRoles);

            _rightCache.put(cacheEntry);
            return rights;
        } else {
            return cached.get();
        }
    }

    private Set<Class<? extends Right>> getApiKeyUserRights() {
        UserFacade user = _facadeService.getFacade(Current.getUser(), UserFacade.class);
        ApiKeySystem apiKey = ComponentSupport.get(user.getParentId(), ApiKeySystem.class);
        return CollectionConverters.asJava(apiKey.getRightClasses(this));
    }

    /**
     * Much like {@link #getUserRights(Id)}, but that the context's parent(s) are also traversed, and the user's rights
     * in those parent(s) are included in the return value. Currently the only considered "parent" is the domain.
     */
    public Set<Class<? extends Right>> getUserRightsInPedigree(Id context) {

        final SecurityContext securityContext = new SecurityContext();
        securityContext.put(CollectedRights.class, new CollectedRights());

        _collectedAuthorityManager.collectAuthorities(securityContext, Current.getDomainDTO());
        _collectedAuthorityManager.collectAuthorities(securityContext, context);

        return securityContext.get(CollectedRights.class).getRights();
    }

    /**
     * Get the rights of the given role in the domain.
     */
    // TODO: Cache /this/
    public Set<Class<? extends Right>> getRoleRights(Id role) {
        return getRoleRights(Current.getDomainDTO(), role);
    }

    /**
     * Designated getRoleRights method.
     */
    public Set<Class<? extends Right>> getRoleRights(Id context, @Nullable Id role) {
        RoleRightsProvider provider;

        if (ComponentSupport.isSupported(RoleRightsProvider.class, _itemService.get(context.getId()))) {
            provider = ComponentSupport.get(context.getId(), RoleRightsProvider.class);
        } else {
            provider = ComponentSupport.get(DefaultRoleRightsProvider.class);
        }

        return provider.getRoleRights(context, role);
    }

    /**
     * Expands the given rightIds according to the rights tree. Hides the rights tree from clients. Given the right
     * hierarchy example in RightModel, then:
     * <pre>
     *     expandRightIds("Grade", "Author") &rarr; [Grade, Author]
     *     expandRightIds("Teach")           &rarr; [Teach, Grade, Author]
     *     expandRightIds("Teach", "-Grade") &rarr; [Teach, Author]
     * </pre>
     */
    public Set<Class<? extends Right>> expandRightIds(Collection<? extends String> rightIds) {

        // partition rightIds into on and off rights (based on "-" prefix), and get the right's class
        Set<Class<? extends Right>> add = new HashSet<>(), remove = new HashSet<>();
        for (String info : rightIds) {
            boolean off = info.startsWith("-");
            String name = off ? info.substring(1) : info;
            Class<? extends Right> right = getRight(name);
            if (right != null) {
                (off ? remove : add).add(right);
            }
        }

        // then mask the whole rights tree (i.e. start with Right.class) with add and remove
        return expandRightIdsIter(add, remove, Right.class, false);
    }

    /**
     * Helper method for {@link #expandRightIds(java.util.Collection)}
     * Iterates over the subtree of {@code right} as defined by {@code __rightTree}, flattening the subtree while
     * removing any subtrees of
     * {@code right} that are turned off (i.e. in {@code remove})
     *
     * @param add       the rights (and subtrees) that are turned on
     * @param remove    the rights (and subtrees) to subtract from those turned on in {@code add}
     * @param right     the tree node of the iteration
     * @param rightIsOn whether or not the tree node of the iteration is on
     */
    private Set<Class<? extends Right>> expandRightIdsIter(Set<Class<? extends Right>> add,
            Set<Class<? extends Right>> remove, Class<? extends Right> right, boolean rightIsOn) {

        final Set<Class<? extends Right>> rights = new HashSet<>();
        rightIsOn = (rightIsOn || add.contains(right)) && !remove.contains(right);
        if (rightIsOn) {
            rights.add(right);
        }
        for (Class<? extends Right> child : getChildren(right)) {
            rights.addAll(expandRightIdsIter(add, remove, child, rightIsOn));
        }

        return rights;
    }

    /**
     * Collapses the given rightIds according to rights tree. Given the right hierarchy example in
     * RightModel,, then
     *
     * <pre>
     *     collapseRightIds("Grade", "Author")          &rarr; [Grade, Author]
     *     collapseRightIds("Teach", "Grade", "Author") &rarr; [Teach]
     *     collapseRightIds("Teach", "Author")          &rarr; [Teach, -Grade]
     * </pre>
     */
    public Set<String> collapseRightIds(final Collection<? extends String> rightIds) {

        // just to make a nice message on the exception if 'rightIds' has any invalid rightIds.
        final List<String> unknownRights = new ArrayList<>(rightIds);

        final Set<Class<? extends Right>> rights = new HashSet<>();
        for (Class<? extends Right> right : getAllRights()) {
            if (rightIds.contains(right.getName())) {
                rights.add(right);
                unknownRights.remove(right.getName());
            }
        }

        if (!unknownRights.isEmpty()) {
            throw new IllegalArgumentException("Invalid rightId(s): " + unknownRights);
        }

        return collapseRightIdsIter(rights, Right.class, false);

    }

    /**
     * Helper method for {@link #collapseRightIds(java.util.Collection)}.
     */
    private Set<String> collapseRightIdsIter(Set<Class<? extends Right>> explicitRights,
            Class<? extends Right> right, boolean parentOn) {
        boolean on = explicitRights.contains(right);
        final Set<String> rights = new HashSet<>();
        if (on && !parentOn) {
            rights.add(right.getName());
        } else if (!on && parentOn) {
            rights.add("-" + right.getName());
        }

        for (Class<? extends Right> childRight : getChildren(right)) {
            rights.addAll(collapseRightIdsIter(explicitRights, childRight, on));
        }

        return rights;
    }

    /**
     * @return true if the current user's rights in the domain include all the *admin* rights of the given user in the domain,
     * false otherwise.
     */
    public boolean isSuperiorToUser(UserFacade user) {
        UserDTO currUser = Current.getUserDTO();
        boolean isSubtenantAdmin = currUser.getSubtenantId().isDefined();
        boolean sameSubtenant = OptionConverters.toJava(currUser.getSubtenantId()).equals(user.getSubtenant().map(Facade::getId));
        return (!isSubtenantAdmin || sameSubtenant) && getUserRights().containsAll(adminRightsOf(getUserRights(Current.getDomainDTO(), user)));
    }

    /**
     * @return true if the current user's rights in the domain include all the *admin* rights of the given role (also in the
     * domain), false otherwise.
     */
    public boolean isSuperiorToRole(Id role) {
        return getUserRights(Current.getDomainDTO()).containsAll(adminRightsOf(getRoleRights(role)));
    }

    public boolean isAdminRole(Id role) {
        return getRoleRights(role).stream().anyMatch(AdminRight.class::isAssignableFrom);
    }

    private Set<Class<? extends Right>> adminRightsOf(Set<Class<? extends Right>> rights) {
        return rights.stream().filter(AdminRight.class::isAssignableFrom).collect(Collectors.toSet());
    }

    public String getRightDescription(Set<Class<? extends Right>> rights) {
        List<String> list = new ArrayList<>();
        getRightDescription(list, rights, Right.class);
        Collections.sort(list);
        return list.isEmpty() ? "None" : StringUtils.join(list, ", ");
    }

    private void getRightDescription(List<String> list, Set<Class<? extends Right>> rights,
            Class<? extends Right> right) {
        if (rights.contains(right)) {
            RightBinding binding = getRightBinding(right);
            String name = ComponentUtils.i18n(binding.name(), getComponentDescriptor());
            if (!rights.containsAll(getDescendants(right))) {
                name = name + "*";
            }
            list.add(name);
        } else {
            for (Class<? extends Right> child : getChildren(right)) {
                getRightDescription(list, rights, child);
            }
        }
    }

    /**
     * @return true if the current user has the given right in the domain, false otherwise
     */
    public boolean getUserHasRight(Class<? extends Right> right) {
        return getUserHasRight(right, RightMatch.EXACT);
    }

    public boolean getUserHasRight(Id context, Id user, Class<? extends Right> right, RightMatch match) {
        Set<Class<? extends Right>> userRights = getUserRights(context,user);
        if (match == RightMatch.EXACT) {
            return userRights.contains(right);
        } else {
            Set<Class<? extends Right>> rightTree = getDescendants(right);
            if (match == RightMatch.ANY) {
                return !Collections.disjoint(userRights, rightTree);
            } else {
                return userRights.containsAll(rightTree);
            }
        }
    }

    public boolean getUserHasRight(Class<? extends Right> right, RightMatch match) {
        return getUserHasRight(Current.getDomainDTO(), Current.getUserDTO(),right,match);
    }

    /**
     * @return true if the current user has at least one of the given rights in the domain, false otherwise
     */
    public boolean getUserHasAtLeastOneRight(Set<Class<? extends Right>> rights) {
        Set<Class<? extends Right>> userRights = getUserRights(Current.getDomainDTO());
        for (Class<? extends Right> right : rights) {
            if (userRights.contains(right)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Designated getUserHasRight method.
     *
     * @return true if the current user has the given right in the given context, false otherwise
     */
    public boolean getUserHasRight(Id context, Class<? extends Right> right) {
        return getUserRights(context).contains(right);
    }

    public boolean getUserHasRight(Id context, Id user, Class<? extends Right> right) {
        return getUserRights(context, user).contains(right);
    }

    /**
     * @return true if the current user has at least one of the given rights in the given context, false otherwise
     * @deprecated Use {@link #getUserHasAtleastOneRight(Id, UserComponent, Set)} instead.
     */
    @Deprecated
    public boolean getUserHasAtLeastOneRight(Id context, Set<Class<? extends Right>> rights) {
        Set<Class<? extends Right>> userRights = getUserRights(context);
        for (Class<? extends Right> right : rights) {
            if (userRights.contains(right)) {
                return true;
            }
        }

        return false;
    }

    public boolean getUserHasAtLeastOneRight(Id context, UserDTO user, Set<Class<? extends Right>> rights) {
        return getUserRights(context, user).stream().anyMatch(rights::contains);
    }

    public boolean getUserHasAtleastOneRight(Id context, UserComponent user, Set<Class<? extends Right>> rights) {
        return getUserRights(context, user).stream().anyMatch(rights::contains);
    }

    public Iterable<Class<? extends Right>> getAllRights() {
        return _environment.getRegistry().lookupAllClasses(Right.class);
    }

    public Class<? extends Right> getRight(String name) {
        return _environment.getRegistry().lookupClass(Right.class, name);
    }

    public RightBinding getRightBinding(Class<? extends Right> right) {
        return right.getAnnotation(RightBinding.class);
    }

    /**
     * Get a map to the additional rights granted by a given right within a given right tree.
     *
     * @param base only consider rights within this tree
     */
    public Multimap<Class<? extends Right>, Class<? extends Right>> getRightMap(final Class<? extends Right> base) {
        return Multimaps.filterKeys(getRightModel().rightMap, c -> c.isAssignableFrom(base));
    }

    /**
     * @return the declaring subclasses of the given right
     */
    public Set<Class<? extends Right>> getChildren(Class<? extends Right> right) {
        return getRightModel().rightTree.get(right);
    }

    /**
     * @return the subclasses of the given right
     */
    public Set<Class<? extends Right>> getDescendants(Class<? extends Right> right) {
        return getRightModel().rightMap.get(right);
    }

    @Bootstrap("core.rights.set")
    public void setRights(JsonRights json) throws Exception {
        logger.log(Level.INFO, "Set rights {0}", json.roleId);

        SupportedRoleFacade supported = null;
        for (SupportedRoleFacade s : _roleService.getSupportedRoles()) {
            if (json.roleId.equals(s.getRole().getRoleId())) {
                supported = s;
            }
        }
        if (supported == null) {
            throw new Exception("Unsupported role: " + json.roleId);
        }

        for (String info : json.rights) {
            boolean negative = info.startsWith("-");
            String name = negative ? info.substring(1) : info;
            Class<? extends Right> right = getRight(name);
            if (right == null) {
                throw new Exception("Unknown right: " + info);
            }
        }
        SupportedRoleFacade.RightsList rights = new SupportedRoleFacade.RightsList();
        rights.addAll(json.rights);
        supported.setRights(rights);
    }

    public static class JsonRights {
        public String roleId;
        public List<String> rights;
    }

    private RightModel getRightModel() {
        return _environment.getAttribute(RightModel.class, () -> new RightModel(getAllRights()));
    }

    private static class RightModel {

        /**
         * Immediate children of a given right.
         */
        final HashMultimap<Class<? extends Right>, Class<? extends Right>> rightTree = HashMultimap.create();

        /**
         * All descendants of a given right.
         */
        final HashMultimap<Class<? extends Right>, Class<? extends Right>> rightMap = HashMultimap.create();

        /**
         * Where the Java inheritance tree is mutilated into the tree shape we actually need. Suppose:
         * <pre>
         *     class Grade extends Teach
         *     class Author extends Teach
         *     class Teach extends Right
         *     class Learn extends Right
         * </pre>
         * then {@code __rightTree} is
         * <pre>
         *     Teach &rarr; [Grade, Author]
         *     Right &rarr; [Teach, Learn]
         * </pre>
         * and {@code __rightMap} is
         * <pre>
         *     Grade  &rarr; [Grade]
         *     Teach  &rarr; [Grade, Author, Teach]
         *     Right  &rarr; [Grade, Author, Teach, Learn]
         *     Author &rarr; [Author]
         *     Learn  &rarr; [Learn]
         * </pre>
         */
        @SuppressWarnings("unchecked")
        RightModel(Iterable<Class<? extends Right>> allRights) {
            for (Class<? extends Right> right : allRights) {
                rightTree.put((Class<? extends Right>) right.getSuperclass(), right);
                Class<? extends Right> cur = right;
                do {
                    rightMap.put(cur, right);
                    cur = (Class<? extends Right>) cur.getSuperclass();
                } while (!cur.equals(Object.class));
            }
        }

    }
}
