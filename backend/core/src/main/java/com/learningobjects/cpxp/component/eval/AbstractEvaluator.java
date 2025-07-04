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

package com.learningobjects.cpxp.component.eval;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Required;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.scala.cpxp.Reattach;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.facade.FacadeSupport;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.lang.OptionLike;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.reflect.TypeUtils;
import scala.collection.Seq;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractEvaluator  implements Evaluator {
    private static final Logger log = Logger.getLogger(AbstractEvaluator.class.getName());
    protected static enum ItemClass {
        FacadeType, ComponentType, ItemType, FinderType, IdType, ReattachType;

        public static ItemClass forClass(Class<?> clas) {
            if (ComponentInterface.class.isAssignableFrom(clas)) {
                return ComponentType;
            } else if (Facade.class.isAssignableFrom(clas)) {
                return FacadeType;
            } else if (Finder.class.isAssignableFrom(clas)) {
                return FinderType;
            } else if ((clas == Item.class) || (clas == Id.class)) {
                return ItemType;
            } else if (clas == Long.class) {
                return IdType;
            } else if (clas == Reattach.class) {
                return ReattachType;
            } else {
                return null;
            }
        }
    }

    protected DelegateDescriptor _delegate;
    protected String _name;
    protected Type _type;
    protected boolean _array;
    protected Class<?> _rawType;
    protected Class<?> _subtype;
    protected ItemClass _itemClass;
    protected String _itemType;
    protected Annotation[] _annotations;
    protected boolean _required;

    @Override
    public void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations) {
        _delegate = delegate;
        _name = name;
        _type = type;
        try {
            Class<?> clas;
            if (type instanceof ParameterizedType) { // List<String> or Set<String>
                ParameterizedType parameterized = (ParameterizedType) type;
                _rawType = (Class<?>) parameterized.getRawType();
                if (Collection.class.isAssignableFrom(_rawType) || Seq.class.isAssignableFrom(_rawType) || OptionLike.isOptionLike(_rawType)) {
                    clas = (Class<?>) parameterized.getActualTypeArguments()[0];
                } else {
                    clas = _rawType;
                }
            } else if (type instanceof GenericArrayType) { // String[]
                GenericArrayType array = (GenericArrayType) type;
                clas = (Class<?>) array.getGenericComponentType();
                _array = true;
            } else if (type instanceof TypeVariable) { // T
                clas = TypeUtils.getRawType(type, delegate.getDelegateClass());
            } else {
                clas = (Class<?>) type; // String
                _array = clas.isArray();
                if (_array) { // String[]
                    clas = clas.getComponentType();
                }
            }
            _subtype = clas;
            _itemClass = ItemClass.forClass(_subtype);
            if (_itemClass == ItemClass.FacadeType || _itemClass == ItemClass.ReattachType) {
                _itemType = FacadeSupport.getItemType((Class<? extends Facade>) _subtype);
            } else if (_itemClass == ItemClass.ComponentType) {
                _itemType = ComponentSupport.getItemType((Class<? extends ComponentInterface>) _subtype);
            }
        } catch (Exception ex) {
            // just try what i support and ignore on throw
            // WHY? This silently hides failures... because there are many
            // ignorable ones such as @Json params etc.
            if(log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Exception initializing " + getClass().getSimpleName() + " for " + name + "/" + delegate.getDelegateClass().getName());
            }
        }
        _annotations = annotations;
        _required = (getAnnotation(Required.class) != null);
        init();
    }

    protected void init() {
    }

    @Override
    public Type getType() {
        return _type;
    }

    protected <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation annotation : _annotations) {
            if (annotationClass.isInstance(annotation)) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    @Override
    public Object getValue(ComponentInstance instance, Object object, Map<String, Object> parameters) {
        return getValue(instance, object);
    }

    @Override
    public Object decodeValue(ComponentInstance instance, Object object, HttpServletRequest request) {
        return getValue(instance, object);
    }

    protected Object getValue(ComponentInstance instance, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParameterName() {
        return null;
    }

    protected Object getItem(Long id, String itemType) {
        if (id == null) {
            return null;
        }
        if (itemType == null) {
            itemType = _itemType;
        }
        switch (_itemClass) {
          case FacadeType:
              return ServiceContext.getContext().getService(FacadeService.class)
                .getFacade(id, itemType, (Class<? extends Facade>) _subtype);
          case ComponentType:
              return ComponentSupport.get(id, itemType, (Class<? extends ComponentInterface>) _subtype);
          case ItemType:
              return ServiceContext.getContext().getItemService().get(id, itemType);
          case FinderType:
              return ServiceContext.getContext().getItemService().get(id, itemType).getFinder();
          case ReattachType:
              return Reattach.apply(ServiceContext.getContext().getItemService().get(id, itemType).getFinder());
          default:
              return id;
        }
    }
}


