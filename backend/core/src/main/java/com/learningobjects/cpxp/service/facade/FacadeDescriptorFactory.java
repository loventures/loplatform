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

import com.google.common.collect.MapMaker;
import com.learningobjects.cpxp.component.ComponentInstanceImpl;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.facade.FacadeUtils.MethodCategory;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.lang.OptionLike;
import com.learningobjects.de.web.jackson.JacksonReflectionUtils;
import scaloi.GetOrCreate;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory for producing facade descriptors.
 */
public class FacadeDescriptorFactory {
    private FacadeDescriptorFactory() {
    }
    private static final ConcurrentMap<Class<?>, Map<String, FacadeDescriptor>> __descriptors = new MapMaker().weakKeys().makeMap();

    private static final ConcurrentMap<Class<?>, String> __facadeTypes = new MapMaker().weakKeys().makeMap();

    public static synchronized String getFacadeType(Class<? extends Facade> facadeClass) {
        String type = __facadeTypes.get(facadeClass);
        if (type == null) {
            Class<?> c = facadeClass;
            FacadeItem facadeItem = c.getAnnotation(FacadeItem.class);
            while (facadeItem == null) {
                Class<?>[] ifaces = c.getInterfaces();
                if (ifaces.length == 0) {
                    throw new RuntimeException("Missing item type: " + facadeClass.getName());
                }
                c = ifaces[0];
                facadeItem = c.getAnnotation(FacadeItem.class);
            }
            type = facadeItem.value();
            if ("".equals(type)) {
                type = StringUtils.substringBeforeLast(c.getSimpleName(), "Facade");
            }
            __facadeTypes.put(facadeClass, type);
        }
        return type;
    }

    public static synchronized FacadeDescriptor getDescriptor
            (String itemType, Class<? extends Facade> facadeClass) {
        Map<String, FacadeDescriptor> map = __descriptors.get(facadeClass);
        FacadeDescriptor descriptor = (map == null) ? null : map.get(itemType);
        if (descriptor == null) {
            String expectedType = getFacadeType(facadeClass);
            if ("*".equals(itemType)) { // hack.. ramifications?
                itemType = expectedType;
            }
            if (!"*".equals(expectedType) && !expectedType.equals(itemType)) {
                throw new RuntimeException
                     ("Error while trying to bind facade type " + facadeClass.getName() + " to item of type " +
                      itemType + ". Facade expects type " + expectedType);
            }

            try {
                descriptor = buildDescriptor(itemType, facadeClass);
                if (map == null) {
                    __descriptors.put(facadeClass, map = new HashMap<>());
                }
                map.put(itemType, descriptor);
            } catch (Exception ex) {
                throw new RuntimeException("Error building facade descriptor: " + itemType + '/' + facadeClass.getName(), ex);
            }
        }
        return descriptor;
    }

    // see: http://grepcode.com/file/repo1.maven.org/maven2/org.apache.cxf/cxf-rt-frontend-jaxrs/2.7.5/org/apache/cxf/jaxrs/utils/InjectionUtils.java#InjectionUtils
    public static Class<?> getActualType(Type genericType, int pos) {

        if (genericType == null) {
            return null;
        }
        if (ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            ParameterizedType paramType = (ParameterizedType)genericType;
            Type t = getType(paramType.getActualTypeArguments(), pos);
            if (t instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) t).getRawType();
            } else if (t instanceof TypeVariable) {
                return (Class<?>) ((TypeVariable<?>) t).getBounds()[0];
            } else if (t instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType)t;
                Type[] bounds = wildcardType.getLowerBounds();
                if (bounds.length == 0) {
                    bounds = wildcardType.getUpperBounds();
                }
                return (Class<?>) bounds[0];
            } else {
                return (Class<?>) t;
            }
        } else {
            if (genericType instanceof TypeVariable) {
                genericType = getType(((TypeVariable<?>)genericType).getBounds(), pos);
            } else if (genericType instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType)genericType;
                Type[] bounds = wildcardType.getLowerBounds();
                if (bounds.length == 0) {
                    bounds = wildcardType.getUpperBounds();
                }
                genericType = getType(bounds, pos);
            }

            Class<?> cls = (Class<?>)genericType;
            return cls.isArray() ? cls.getComponentType() : cls;
        }
    }

    public static Type getType(Type[] types, int pos) {
        if (pos >= types.length) {
            throw new RuntimeException("No type can be found at position " + pos);
        }
        return types[pos];
    }

    private static boolean isGetCategory(final Method method) {
        final MethodCategory methodCategory = FacadeUtils.MethodCategory.getMethodCategory(method.getName());
        return MethodCategory.GET.equals(methodCategory);
    }

    // Encapsulates type check for return types based on annotation.
    private static class AnnotationReturnTypeCheck <A extends Annotation> {

        private final Class<A> annotation;
        private final Class[] expectedTypes;

        AnnotationReturnTypeCheck(Class<A> annotation,
                                  Class... assignableFrom) {
            this.annotation     = annotation;
            this.expectedTypes = assignableFrom;
        }

        boolean isViolated(Method method, Class<?> foundResultType) {
            if (method.isAnnotationPresent(this.annotation)) {
                for (Class<?> c : this.expectedTypes) {
                    if (c.isAssignableFrom(foundResultType)) {
                        return false;
                    }
                }
                return true; // Violation. Has annotation, but no type match.
            }
            return false;
        }
    }

    // Add any return type checking you want on a per-annotation basis
    private static final AnnotationReturnTypeCheck[] getCategoryTypeChecks = {
            new AnnotationReturnTypeCheck<>(FacadeComponent.class, ComponentInterface.class, ComponentInstanceImpl.class, Long.class, Long.TYPE),
            new AnnotationReturnTypeCheck<>(FacadeChild.class, ComponentInterface.class, Facade.class, Long.class, Long.TYPE),
            new AnnotationReturnTypeCheck<>(FacadeParent.class, ComponentInterface.class, Facade.class, Long.class, Long.TYPE)
    };

    private static FacadeDescriptor buildDescriptor
    (String itemType, Class<? extends Facade> facadeClass) throws Exception {
        Ontology ontology = BaseOntology.getOntology();
        EntityDescriptor descriptor = ontology.getEntityDescriptor(itemType);

        FacadeDescriptor facadeDescriptor = new FacadeDescriptor(descriptor);
        facadeDescriptor.addHandler(new ItemIdHandler());
        facadeDescriptor.addHandler(new ParentIdHandler());
        facadeDescriptor.addHandler(new RootIdHandler());
        facadeDescriptor.addHandler(new ItemTypeHandler());
        facadeDescriptor.addHandler(new ItemPathHandler());
        facadeDescriptor.addHandler(new RefreshHandler());
        facadeDescriptor.addHandler(new PolluteHandler());
        facadeDescriptor.addHandler(new RemoveHandler());
        facadeDescriptor.addHandler(new DeleteHandler());
        facadeDescriptor.addHandler(new ItemHandler());
        facadeDescriptor.addHandler(new LockHandler());
        facadeDescriptor.addHandler(new InvalidateHandler());
        facadeDescriptor.addHandler(new InvalidateParentHandler());
        facadeDescriptor.addHandler(new ToStringHandler(facadeClass));
        facadeDescriptor.addHandler(new EqualsHandler());
        facadeDescriptor.addHandler(new HashCodeHandler());
        facadeDescriptor.addHandler(new BindUrlHandler());
        facadeDescriptor.addHandler(new AsFacadeHandler());

        //first pass -- collect annotations
        for (Method method: facadeClass.getMethods()) {
            if(method.isDefault()){
                //Default methods are handled more directly
                //Java 8 added a 'feature' that methods with less specific return types inherited from a parent interface
                //are implemented as default methods that just call the more specific version. This is transparent
                //now that default methods are being handled. It does introduce some further redirection but probably
                //not normally a noticeable slow-down compared to all the rest of the stuff done here.
                continue;
            }
            String groupName = FacadeUtils.getNameForHandlerGroupWithThisMethod(method);
            if (StringUtils.isNotBlank(groupName)) {
                if (method.isAnnotationPresent(FacadeData.class)) {
                    FacadeData facadeData = method.getAnnotation(FacadeData.class);
                    facadeDescriptor.setFacadeDataForHandlerGroup(facadeData, groupName);
                } else if (method.isAnnotationPresent(FacadeJson.class)) {
                    FacadeJson facadeJson = method.getAnnotation(FacadeJson.class);
                    facadeDescriptor.setFacadeJsonForHandlerGroup(facadeJson, groupName);
                } else if (method.isAnnotationPresent(FacadeChild.class)) {
                    FacadeChild facadeChild = method.getAnnotation(FacadeChild.class);
                    facadeDescriptor.setFacadeChildForHandlerGroup(facadeChild, groupName);
                } else if (method.isAnnotationPresent(FacadeParent.class)) {
                    FacadeParent facadeParent = method.getAnnotation(FacadeParent.class);
                    facadeDescriptor.setFacadeParentForHandlerGroup(facadeParent, groupName);
                } else if (method.isAnnotationPresent(FacadeStorage.class)) {
                    FacadeStorage facadeStorage = method.getAnnotation(FacadeStorage.class);
                    facadeDescriptor.setFacadeStorageHandlerGroup(facadeStorage, groupName);
                } else if (method.isAnnotationPresent(FacadeComponent.class)) {
                    FacadeComponent facadeComponent = method.getAnnotation(FacadeComponent.class);
                    facadeDescriptor.setFacadeComponentForHandlerGroup(facadeComponent, groupName);
                }
                Class<?> resultType = method.getReturnType();
                if ((Collection.class.isAssignableFrom(resultType)
                      || scala.collection.Iterable.class.isAssignableFrom(resultType)
                      || OptionLike.isOptionLike(resultType)) && (method.getGenericReturnType() instanceof ParameterizedType)
                      || GetOrCreate.class.isAssignableFrom(resultType)) {
                    resultType = getActualType(method.getGenericReturnType(), 0);
                }
                if (ComponentInterface.class.isAssignableFrom(resultType)) {
                    //noinspection unchecked
                    facadeDescriptor.setComponentInterfaceForHandlerGroup((Class<? extends ComponentInterface>) resultType, groupName);
                }
                if (Facade.class.isAssignableFrom(resultType)) {
                    //noinspection unchecked
                    facadeDescriptor.setFacadeForHandlerGroup((Class<? extends Facade>) resultType, groupName);
                }

                // Look for expected return types for getters.
                // Prevent derps like forgetting to extend ComponentInterface or Facade.
                if (isGetCategory(method)) {
                    for (AnnotationReturnTypeCheck<?> typeCheck : getCategoryTypeChecks) {
                        if (typeCheck.isViolated(method, resultType)) {
                            final String msg = "Cannot create handler, unrecognized return type <"+resultType.getName()+"> for method: "+facadeClass.getName()+"#"+method.getName();
                            throw new RuntimeException(msg);
                        }
                    }
                }
            }
        }

        FacadeMethodHandlerFactory facadeMethodHandlerFactory = new FacadeMethodHandlerFactory(itemType, ontology, descriptor, facadeDescriptor);
        // second pass -- adding invocation handlers, using annotation lookups
        for (Method method: facadeClass.getMethods()) {
            if (method.isDefault()) {
                continue; //Don't intercept default methods
            }

            FacadeMethodHandler methodHandler = null;

            final String methodName = method.getName();

            MethodCategory methodCategory = FacadeUtils.MethodCategory.getMethodCategory(methodName);
            if (methodCategory != null) {
                methodHandler = facadeMethodHandlerFactory.createFacadeMethodHandler(method, methodName, methodCategory);
            }

            if (methodHandler != null) {
                facadeDescriptor.addHandler(method, methodHandler);
            }
        }
        return facadeDescriptor;
    }

    private static class FacadeMethodHandlerFactory {
        private String itemType;
        private Ontology ontology;
        private EntityDescriptor descriptor;
        private FacadeDescriptor facadeDescriptor;

        public FacadeMethodHandlerFactory(String itemType, Ontology ontology, EntityDescriptor descriptor, FacadeDescriptor facadeDescriptor) {
            this.itemType = itemType;
            this.ontology = ontology;
            this.descriptor = descriptor;
            this.facadeDescriptor = facadeDescriptor;
        }

        public FacadeMethodHandler createFacadeMethodHandler(Method method, String methodName, MethodCategory methodCategory) throws Exception {
            String groupName = methodCategory.getNameForHandlerGroupWithThisMethod(method);
            String singular = StringUtils.toLowerCaseFirst(FacadeUtils.singularize(groupName)); // FFS I weep
            FacadeData facadeData = facadeDescriptor.getFacadeDataForHandlerGroup(groupName);
            FacadeJson facadeJson = facadeDescriptor.getFacadeJsonForHandlerGroup(groupName);
            FacadeChild facadeChild = method.getAnnotation(FacadeChild.class);
            FacadeComponent facadeComponent = method.getAnnotation(FacadeComponent.class);
            if (facadeChild == null) {
                facadeChild = facadeDescriptor.getFacadeChildForHandlerGroup(groupName);
            }
            if (facadeComponent == null) {
                facadeComponent = facadeDescriptor.getFacadeComponentForHandlerGroup(groupName);
            }
            FacadeParent facadeParent = facadeDescriptor.getFacadeParentForHandlerGroup(groupName);
            FacadeStorage facadeStorage = facadeDescriptor.getFacadeStorageForHandlerGroup(groupName);
            Class<? extends Facade> facade = facadeDescriptor.getFacadeForHandlerGroup(groupName);
            Class<? extends ComponentInterface> componentInterface = facadeDescriptor.getComponentInterfaceForHandlerGroup(groupName);

            FacadeMethodHandler methodHandler = null;

            switch (methodCategory) {
                case ADD:        //TODO/TECH DEBT: at some point, encapsulate this behavior polymorphically in the enum classes themselves, rather than using a switch
                {
                    if (facadeChild != null) {
                        methodHandler = new ChildAddHandler(method, facadeChild, facade);
                    } else if (facadeData != null) {
                        String typeName = getDataType(facadeData, singular, false);
                        DataFormat format = ontology.getDataFormat(typeName);
                        if (descriptor == null || !descriptor.containsDataType(typeName)) {
                            methodHandler = new DataAddHandler(format, method, facadeData, typeName);
                        } else {
                            throw new RuntimeException("Invalid add/remove method for entity mapped data: " + typeName);
                        }
                    } else if (componentInterface != null) {
                        methodHandler = new FacadeComponentHandler(method, componentInterface, facadeComponent);
                    }
                }
                break;
                case COUNT:
                case FIND:
                {
                    if (facadeChild != null) {
                        FacadeQuery facadeQuery = method.getAnnotation(FacadeQuery.class);
                        methodHandler = new ChildGetHandler(method, facadeChild, facade, false, facadeQuery);
                    } else if (componentInterface != null) {
                        methodHandler = new FacadeComponentHandler(method, componentInterface, facadeComponent);
                    }
                }
                break;
                case GET:
                case IS:
                {
                    if (facadeData != null) {
                        boolean isLong = Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType());
                        String typeName = getDataType(facadeData, singular, isLong);
                        DataFormat format = ontology.getDataFormat(typeName);
                        if (descriptor == null || !descriptor.containsDataType(typeName)) {
                            methodHandler = new DataGetHandler(format, method, facadeData, typeName);
                        } else {
                            methodHandler = new EntityGetHandler(descriptor, format, method, facadeData, typeName);
                        }
                    } else if (facadeJson != null) {
                        String property = JacksonReflectionUtils.getPropertyName(method).orElse(null);
                        methodHandler = new FacadeJsonHandler(method, property, facadeJson, ontology);
                    } else if (facadeChild != null) {
                        final Class<?>[] parameterTypes = method.getParameterTypes();
                        FacadeQuery facadeQuery = method.getAnnotation(FacadeQuery.class);
                        methodHandler = new ChildGetHandler(method, facadeChild, facade, parameterTypes != null && parameterTypes.length > 0, facadeQuery);
                    } else if (facadeParent != null) {
                        methodHandler = new ParentGetHandler(method, facadeParent);
                    } else if (facadeStorage != null) {
                        String storageName = facadeStorage.value();
                        methodHandler = new FacadeStorageHandler(storageName, methodName, false);
                    } else if (componentInterface != null) {
                        methodHandler = new FacadeComponentHandler(method, componentInterface, facadeComponent);
                    }
                }
                break;
                case GETORCREATE:
                {
                    if (facadeChild != null) {
                        methodHandler = new ChildGetOrCreateHandler(method, facadeChild, facade);
                    } else if (componentInterface != null) {
                        methodHandler = new FacadeComponentHandler(method, componentInterface, facadeComponent);
                    }
                }
                break;
                case QUERY:
                {
                    FacadeQuery facadeQuery = method.getAnnotation(FacadeQuery.class);
                    if (facadeChild != null) {
                        methodHandler = new ChildGetHandler(method, facadeChild, facade, false, facadeQuery);
                    } else if (componentInterface != null) {
                        methodHandler = new FacadeComponentHandler(method, componentInterface, facadeComponent); // facadeQuery
                    }
                }
                break;
                case REMOVE:
                {
                    if (facadeChild != null) {
                        methodHandler = new ChildRemoveHandler(method, facadeChild, facade);
                    } else if (facadeData != null) {
                        String typeName = getDataType(facadeData, singular, false);
                        DataFormat format = ontology.getDataFormat(typeName);
                        if (descriptor == null || !descriptor.containsDataType(typeName)) {
                            methodHandler = new DataRemoveHandler(format, method, facadeData, typeName);
                        } else {
                            throw new RuntimeException("Invalid add/remove method for entity mapped data: " + typeName);
                        }
                    } else if (componentInterface != null) {
                        methodHandler = new FacadeComponentHandler(method, componentInterface, facadeComponent);
                    } else if (facadeJson != null) {
                        methodHandler = new FacadeJsonHandler(method, singular, facadeJson, ontology);
                    }
                }
                break;
                case SET:
                {
                    if (facadeData != null) {
                        String typeName = getDataType(facadeData, singular, false);
                        DataFormat format = ontology.getDataFormat(typeName);
                        if (descriptor == null || !descriptor.containsDataType(typeName)) {
                            methodHandler = new DataSetHandler(format, method, facadeData, typeName);
                        } else {
                            boolean global = ontology.getDataType(typeName).global();
                            methodHandler = new EntitySetHandler(descriptor, format, method, facadeData, typeName, global);
                        }
                    } else if (facadeJson != null) {
                        String property = JacksonReflectionUtils.getPropertyName(method).orElse(null);
                        methodHandler = new FacadeJsonHandler(method, property, facadeJson, ontology);
                    } else if (facadeStorage != null) {
                        String storageName = facadeStorage.value();
                        methodHandler = new FacadeStorageHandler(storageName, methodName, true);
                    } else if (componentInterface != null) {
                        methodHandler = new FacadeComponentHandler(method, componentInterface, facadeComponent);
                    }
                }
                break;
                default:
                    break;
            }
            return methodHandler;
        }

        private String getDataType(FacadeData facadeData, String singular, boolean isId) {
            String type = facadeData.value();
            if ("".equals(type)) {
                if (isId) {
                    singular = StringUtils.removeEnd(singular, "Id");
                }
                type = itemType + '.' + StringUtils.toLowerCaseFirst(singular);
            }
            return type;
        }
    }
}
