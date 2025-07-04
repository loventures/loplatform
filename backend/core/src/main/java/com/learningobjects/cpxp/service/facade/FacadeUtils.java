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

package com.learningobjects.cpxp.service.facade;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeGroup;
import com.learningobjects.cpxp.dto.FacadeQuery;
import com.learningobjects.cpxp.util.StringUtils;
import scala.collection.Seq;

import java.lang.reflect.Method;
import java.util.List;

public class FacadeUtils {

    public static String pluralize(String name) {
        if (name.toLowerCase().endsWith("child")) return name + "ren";
        return name.endsWith("s") ? name + "es" : name.endsWith("y") ? name.substring(0, name.length() - 1) + "ies" : name.endsWith("x") ? name + "es" : name + "s";
    }

    public static String singularize(String name) {
        int n = name.length();
        if (name.toLowerCase().endsWith("children")) return name.substring(0,n-3);
        return name.endsWith("ies") ? name.substring(0, n -3) + "y"
            : (name.endsWith("ses") || name.endsWith("xes")) ? name.substring(0, n - 2)
            : name.substring(0, n - 1);
    }

    public static String getNameForHandlerGroupWithThisMethod(Method method) {
        MethodCategory methodCategory = MethodCategory.getMethodCategory(method.getName());
        return (methodCategory == null) ? null : methodCategory.getNameForHandlerGroupWithThisMethod(method);
    }

    public enum MethodCategory {
        ADD("add"),
        REMOVE("remove"),
        FIND("find"),
        QUERY("query"),
        COUNT("count"),
        GETORCREATE("getOrCreate"),
        GET("get"),
        IS("is"),
        SET("set");

        MethodCategory(String prefix) { //TODO and MethodExecutionStrategy strategy, to get rid of switch statements
            this.prefix = prefix;
        }

        private final String prefix;

        private static String removeConditionalsFromMethodName(String nameWithoutPrefix) {
            String name = nameWithoutPrefix;

            if (nameWithoutPrefix.contains("By") && !nameWithoutPrefix.endsWith("By")) {
                name = StringUtils.substringBefore(nameWithoutPrefix, "By");
            }
            // horrible hack to support getFooByBar with autocreation, without breaking
            // getMaximumPoints...
            if (name.startsWith("get")) {
                return name;
            }
            if (nameWithoutPrefix.contains("Max")) {
                name = StringUtils.substringBefore(nameWithoutPrefix, "Max"); // TODO: a more generic approach...
            } else if (nameWithoutPrefix.contains("Min")) {
                name = StringUtils.substringBefore(nameWithoutPrefix, "Min");
            }

            return name;
        }

        private boolean isCorrectCategoryFor(String methodName)
        {
            if (prefix == "get" && methodName.startsWith("getOrCreate")) return false; //special case
            return methodName.startsWith(prefix);
        }

        public String trimPrefix(String methodName){
            return methodName.substring(prefix.length());
        }

        public static MethodCategory getMethodCategory(String methodName) {

            for ( final MethodCategory methodCategory: MethodCategory.values( ) ) {
                if (methodCategory.isCorrectCategoryFor(methodName)){
                    return methodCategory;
                }
            }
            return null;
        }

        public String getNameForHandlerGroupWithThisMethod(Method method) {

            FacadeGroup group = method.getAnnotation(FacadeGroup.class);
            if (group != null) {
                return group.value();
            }
            FacadeQuery query = method.getAnnotation(FacadeQuery.class);
            if ((query != null) && !"".equals(query.group())) {
                return query.group();
            }
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            Class<?>[] paramTypes = method.getParameterTypes();
            boolean listParam = paramTypes != null && paramTypes.length > 0 && (List.class.isAssignableFrom(paramTypes[0]) || Seq.class.isAssignableFrom(paramTypes[0]));
            boolean listResult = List.class.isAssignableFrom(returnType) || Seq.class.isAssignableFrom(returnType);

            //TODO during BrownBag -- refactor this
            switch (this) {
                case ADD:
                    String trimmed = trimPrefix(methodName);
                    return (listParam ? trimmed : FacadeUtils.pluralize(trimmed));
                case COUNT:
                    return trimPrefix(removeConditionalsFromMethodName(methodName));
                case FIND:
                    final String findBasename = trimPrefix(removeConditionalsFromMethodName(methodName));
                    return listResult ? findBasename : FacadeUtils.pluralize(findBasename);
                case GET:
                case IS:
                    final String getBasename = trimPrefix(removeConditionalsFromMethodName(methodName));
                    return listResult ? getBasename : FacadeUtils.pluralize(getBasename);
                case GETORCREATE:
                    return trimPrefix(FacadeUtils.pluralize(removeConditionalsFromMethodName(methodName)));
                case QUERY:
                    return trimPrefix(removeConditionalsFromMethodName(methodName));
                case REMOVE:
                    trimmed = trimPrefix(methodName);
                    return (listParam ? trimmed : FacadeUtils.pluralize(trimmed));
                case SET:
                    trimmed = trimPrefix(methodName);
                    return (listParam ? trimmed : FacadeUtils.pluralize(trimmed));
                default:
                    return null;
            }
        }
    }

    public static String getChildType(FacadeChild facadeChild, Class<? extends Facade> facade) {
        String type = facadeChild.value();
        if (StringUtils.isEmpty(type)) {
            type = FacadeDescriptorFactory.getFacadeType(facade);
            if ("*".equals(type)) {
                throw new RuntimeException("Unknown child type: " + facade.getName());
            }
        }
        return type;
    }

    public static String truncateToMaxStringLength(String str) {
        if ((str != null) && (str.length() > 255)) { // TODO: THis is BAD, it causes silent data loss.
            // Prevent names from being longer than the underlying data
            // structure can hold
            str = str.substring(0, 255);
        }
        return str;
    }
}
