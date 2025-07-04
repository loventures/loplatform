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

package com.learningobjects.cpxp.component.internal;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.element.CustomTag;
import com.learningobjects.cpxp.component.eval.*;
import com.learningobjects.cpxp.component.function.Function;
import com.learningobjects.cpxp.component.init.FieldInitializer;
import com.learningobjects.cpxp.component.init.Initializer;
import com.learningobjects.cpxp.component.init.PostConstructInitializer;
import com.learningobjects.cpxp.component.registry.Bound;
import com.learningobjects.cpxp.component.registry.ResourceBinding;
import com.learningobjects.cpxp.component.util.Html;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.scala.cpxp.Reattach;
import com.learningobjects.cpxp.schedule.Scheduled;
import com.learningobjects.cpxp.schedule.ScheduledTask;
import com.learningobjects.cpxp.schedule.Scheduler;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.item.ItemTypedef;
import com.learningobjects.cpxp.service.overlord.OverlordWebService;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable;
import com.learningobjects.cpxp.util.lang.OptionLike;
import com.learningobjects.cpxp.util.lang.ProviderLike;
import de.mon.DeMonitor;
import de.mon.StatisticType;
import jakarta.servlet.http.HttpServletRequest;
import scala.compat.java8.OptionConverters;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BaseDelegateDescriptor extends AbstractDescriptor implements com.learningobjects.cpxp.component.internal.DelegateDescriptor {
    private static final Logger log = Logger.getLogger(BaseDelegateDescriptor.class.getName());
    private final ComponentDescriptor _component;
    private final Class<?> _delegate;
    private final boolean _service;
    private final DelegateDescriptor _parent;
    private final Map<Class<? extends ComponentInterface>, Annotation> _bindings =
            new HashMap<>();
    private final Set<Class<?>> _serviceInterfaces = new HashSet<>();
    private final List<Initializer> _initializers = new ArrayList<>();
    private final Map<String, FunctionDescriptor> _methodRefs = new HashMap<>();
    private final List<Field> _entityTypes = new ArrayList<>();
    private final Map<Method, ScheduledTask> _scheduled = new HashMap<>();
    private Constructor<?> _implCtor;
    private List<Evaluator> _implCtorEvaluators;
    private final List<Field> _stateFields = new ArrayList<>();
    private boolean _instrument;
    private Field _moduleField;

    public BaseDelegateDescriptor(ComponentDescriptor component, Class<?> clas) {
        _component = component;
        _delegate = clas;
        _service = clas.isAnnotationPresent(Service.class);
        _parent = this;
        init();
    }

    public BaseDelegateDescriptor(DelegateDescriptor parent, Class<?> clas) {
        _component = null;
        _delegate = clas;
        _service = false;
        _parent = parent;
        init();
    }

    @Override
    public String toString() {
        return "DelegateDescriptor[" + _delegate.getName() + "]";
    }

    // TODO: Refactor this to be ctor calling setters so the fields can be final
    private void init() {
        // Problems with cross-component delegates include the inability to look up resources
        // if ((_component != null) && !clas.getPackage().equals(_component.getComponentClass().getPackage())) {
        //     throw new Exception("Invalid cross-package delegate: " + clas);
        // }
        try {
            introspect(_delegate, new HashSet<>());
            findConstructors();
            if (_service || !_serviceInterfaces.isEmpty() || ((_component != null) && _component.isStateless())) {
                if (_implCtor != null) {
                    checkSingletonInstantiation();
                }
            }
        } catch (Throwable th) { // some of the introspection exceptions are errors...
            throw new RuntimeException("Error introspecting: " + _delegate.getName(), th);
        }
    }

    @Override
    public ComponentDescriptor getComponent() {
        return _component;
    }

    @Override
    public Class<?> getDelegateClass() {
        return _delegate;
    }

    @Override
    public boolean instrument() {
        return _instrument;
    }

    private void findConstructors() throws Exception {
        if (_delegate.getName().endsWith("$")) {
            _moduleField = _delegate.getDeclaredField("MODULE$");
        } else {
            _implCtor = getConstructor(_delegate);
            _implCtorEvaluators = getEvaluators(_implCtor);
        }
    }

    private static Constructor<?> getConstructor(Class<?> clas) throws Exception {
        Constructor<?> constructor = null;
        for (Constructor<?> ctor: clas.getConstructors()) {
            if (Modifier.isPublic(ctor.getModifiers()) && !ctor.isAnnotationPresent(DeIgnore.class)) {
                if (constructor != null) {
                    throw new Exception("Component class has multiple public constructors: " + clas.getName());
                }
                constructor = ctor;
            }
        }
        if (constructor == null) {
            throw new Exception("Component class has no public constructors: " + clas.getName());
        }
        return constructor;
    }

    private List<Evaluator> getEvaluators(Constructor<?> ctor) {
        List<Evaluator> evaluators = new ArrayList<>();
        Class<?>[] classes = ctor.getParameterTypes();
        Type[] types = ctor.getGenericParameterTypes();
        Annotation[][] annotations = ctor.getParameterAnnotations();
        for (int i = 0; i < types.length; ++ i) {
            Evaluator evaluator = getConstructorEvaluator(classes[i], annotations[i]);
            evaluator.init(this, null, types[i], annotations[i]);
            evaluators.add(evaluator);
        }
        return evaluators;
    }

    private Evaluator getConstructorEvaluator(Class<?> clas, Annotation[] annotations) {
        /* TODO: can we inject anything else? */
        if (clas.isAnnotationPresent(Local.class) || clas.isAnnotationPresent(Service.class)) {
            return new InjectEvaluator();
        } else if (Facade.class.isAssignableFrom(clas)
                   || Finder.class.isAssignableFrom(clas)
                   || Reattach.class.isAssignableFrom(clas) // eh.
                   || ComponentInterface.class.isAssignableFrom(clas) // recklessly
                   || ClassUtils.isAnnotationPresent(Instance.class, annotations)) {
            return new InstanceEvaluator();
        } else if (ClassUtils.isAnnotationPresent(Init.class, annotations)) {
            return new InitEvaluator();
        } else if (Arrays.stream(annotations).anyMatch(ann -> ann instanceof Inject)) {
            return new InjectEvaluator();
        } else if (ProviderLike.isProviderLike(clas) || OptionLike.isOptionLike(clas)) {
            return new InjectEvaluator();
        } else {
            return new InferEvaluator();
        }
    }

    @Override
    public boolean isService() {
        return _service;
    }

    @Override
    public Map<Class<? extends ComponentInterface>, Annotation> getBindings() {
        return _bindings;
    }

    @Override
    public Set<Class<?>> getServiceInterfaces() {
        return _serviceInterfaces;
    }

    @Override
    public <T extends Annotation> T getBinding(
            Class<? extends ComponentInterface> iface) {
        return (T) _bindings.get(iface);
    }

    @Override
    public Object newInstance(ComponentInstance instance) {
        return newInstance(instance, obj -> {});
    }

    @Override
    public Object newInstance(@Nullable ComponentInstance instance, Consumer<Object> cache) {
        long then = System.nanoTime();
        try {
            // Until this is rationalizable, only inject instance args for instance descriptors.
            // Specifically, for constructor-DI components that are created via add with init
            // parameter, the init parameter shouldn't be constructor DI'ed first. Creation
            // parameters should be distinct from get ephemeral instance parameters.
            final int m;
            if (instance == null) {
                m = 0;
            } else {
                m = (_component != null) ? instance.getArgs().length : 0;
            }
            Object object;
            if (_moduleField != null) {
                object = _moduleField.get(null);
            } else {
                int n = _implCtorEvaluators.size();
                Object[] args = new Object[n];
                if (n > 0) {
                    // It would be nice to be able to introspect scala multi-arglist constructors and
                    // throw if insufficient params had been passed.
                    for (int i = 0; i < m; ++i) {
                        args[i] = instance.getArgs()[i];
                    }
                    for (int i = m; i < n; ++i) {
                        args[i] = _implCtorEvaluators.get(i).getValue(instance, null, Collections.<String, Object>emptyMap());
                    }
                }
                object =_implCtor.newInstance(args);
            }
            cache.accept(object);
            if(log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Created new instance of " + object.getClass().getName());
            }
            for (Initializer initializer : _initializers) {
                if(log.isLoggable(Level.FINER)) {
                    log.log(Level.FINER, "Initializing " + object.getClass().getName() + "with " + initializer.getClass().getName());
                }
                initializer.initialize(instance, object);
            }
            DeMonitor.recordGlobalStatistic(StatisticType.Component$.MODULE$, _delegate.getSimpleName(), System.nanoTime() - then);
            return object;
        } catch (Exception ex) {
            throw new RuntimeException("Error instantiating: " + _delegate.getName(), ex);
        }
    }

    private void checkSingletonInstantiation() {
        for (Evaluator evaluator : _implCtorEvaluators) {
            if (!evaluator.isStateless()) {
                throw new RuntimeException("Cannot instantiate " + _delegate.getName() + " as a singleton, since it injects a " + evaluator.getType() + " with a " + evaluator.getClass().getSimpleName());
            }
        }
        for (Initializer initializer : _initializers) {
            if (!initializer.isStateless()) {
                throw new RuntimeException("Cannot instantiate " + _delegate.getName() + " as a singleton, since it contains " + initializer.getTarget());
            }
        }
        if (!_stateFields.isEmpty()) {
            String fields = String.join(", ", _stateFields.stream().map(Objects::toString).collect(Collectors.toList()));
            throw new RuntimeException("Cannot instantiate " + _delegate.getName() + " as a singleton, since it contains non-static non-final field(s) " + fields);
         }
    }

    @Override
    public Object invokeRef(String name, ComponentInstance instance, Object object) {
        FunctionDescriptor fn = _methodRefs.get(name);
        if (fn == null) {
            throw new RuntimeException("No method ref " + name + " on " + _component.getIdentifier());
        }
        try {
            Object[] parameters = new Object[fn.getEvaluators().size()];
            HttpServletRequest request = BaseWebContext.getContext().getRequest();
            for (int i = 0; i < parameters.length; ++ i) {
                Evaluator evaluator = fn.getEvaluators().get(i);
                parameters[i] = evaluator.decodeValue(instance, object, request);
            }
            return fn.getMethod().invoke(object, parameters);
        } catch (Exception ex) {
            throw new RuntimeException("Error evaluating: " + _delegate.getName() + "#" + name, ex);
        }
    }

    @Override
    public Object invokeRef(String name, ComponentInstance instance, Object object, Object[] args) {
        try {
            FunctionDescriptor fn = _methodRefs.get(name);
            return fn.getMethod().invoke(object, args);
        } catch (Exception ex) {
            throw new RuntimeException("Error evaluating: " + _delegate.getName() + "#" + name, ex);
        }
    }

    private void introspect(Class<?> clas, Set<Class<?>> visited) throws Exception {
        addMessages(clas); // Do this low to high so that subclass messages take precedence.

        super.introspectAccess(this, clas);

        _instrument |= clas.isAnnotationPresent(Instrument.class);

        // Force Virtual annotations to reprocess any access controls
        if (_component != null && _delegate.equals(_component.getComponentClass())) {
            for (Annotation annotation : _component.getVirtualAnnotations()) {
                super.introspectAccess(this, annotation);
            }
        }

        for (Field field : clas.getDeclaredFields()) {
            addMessages(field);
            boolean injected = false;
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                Evaluator evaluator =
                  OptionConverters.<Evaluator>toJava(CpxpEvaluators.knownEvaluator(annotation.annotationType())).orElse(null);
                if (evaluator != null) {
                    evaluator.init(_parent, StringUtils.stripStart(field.getName(), "_"), field.getGenericType(), field.getAnnotations());
                    _initializers.add(new FieldInitializer(field, evaluator));
                    injected = true;
                }
                if ((annotation instanceof ItemTypedef) || (annotation instanceof DataTypedef)) {
                    _entityTypes.add(field);
                }
            }
            if (!injected
                    && !Modifier.isStatic(field.getModifiers())
                    && !Modifier.isFinal(field.getModifiers())
                    && !field.getName().endsWith("$module") // scala inner classes
                    && !field.getName().endsWith("$lzy1") // scala lazy vals
                    && !ClassUtils.hasDeclaredMethod(clas, field.getName() + "$lzycompute") // scala lazy vals
                    && !ClassUtils.hasDeclaredField(clas, field.getName() + "$$Provider")
                    && !field.isAnnotationPresent(SingletonState.class)) { // meh
                _stateFields.add(field);
            }
        }
        for (Method method : clas.getDeclaredMethods()) {
            // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.6
            if ((method.getModifiers() & 4096) == 4096) {
                // ignoring synthetic.. I find that Scala 3 duplicates default
                // methods such as `getIntegrationRoot` from the interface onto
                // the implementation (e.g. `LightweightCourseImpl`) which blows
                // up duplicate request mapping checks.
                continue;
            }
            addMessages(method);
            if (method.isAnnotationPresent(MethodRef.class)) {
                addMethodRef(method);
            }
            if (method.isAnnotationPresent(PostConstruct.class)) {
                _initializers.add(new PostConstructInitializer(method));
            }
            if (method.isAnnotationPresent(Scheduled.class)) {
                _scheduled.put(method, null);
            }
            if (method.isAnnotationPresent(Configuration.class) && (_delegate == _component.getComponentClass())) {
                _component.addConfiguration(method);
            }
            if (method.isAnnotationPresent(Tag.class)
                    && !Void.TYPE.equals(method.getReturnType())
                    && !Html.class.isAssignableFrom(method.getReturnType())
                    && !CustomTag.class.isAssignableFrom(method.getReturnType())) {
                throw new Exception("Bogus @Tag: " + method);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                registerFunction(annotation, method);
            }
        }

        for (Class<?> i : clas.getInterfaces()) {

            if (ComponentInterface.class.isAssignableFrom(i)) {
                final Class<? extends ComponentInterface> componentInterface = i.asSubclass(ComponentInterface.class);

                if (_component.addInterface(componentInterface, this)) {

                    recordBindings(componentInterface);

                    // support interface hierarchies declaring bound functions...
                    BreadthFirstSupertypeIterable.from(componentInterface).forEach(iface -> {
                        if (visited.add(iface)) {
                            for (final Method method : iface.getDeclaredMethods()) {
                                addMessages(method);
                                for (final Annotation annotation : method.getDeclaredAnnotations()) {
                                    registerFunction(annotation, method);
                                }
                                if (method.isAnnotationPresent(Configuration.class) && (_delegate == _component.getComponentClass())) {
                                    _component.addConfiguration(method);
                                }
                            }
                            _instrument |= iface.isAnnotationPresent(Instrument.class);
                        }
                    });

                }
            }
            if (i.isAnnotationPresent(Service.class)) {
                _serviceInterfaces.add(i);
            }
            for (final Field field : i.getDeclaredFields()) {
                if (field.isAnnotationPresent(ItemTypedef.class) || field.isAnnotationPresent(DataTypedef.class)) {
                    _entityTypes.add(field);
                }
            }
            _instrument |= i.isAnnotationPresent(Instrument.class);
        }

        if (clas.getSuperclass() != null) {
            introspect(clas.getSuperclass(), visited);
        }
    }

    private Annotation findNarrowestBindingAnnotation(
            final Class<? extends Annotation> bindingAnnotationType) {

        /*
         * ComponentDescriptor sources are not completely abstracted away.
         * They don't all come from @Component-annotated Java types. Some are
         * "virtual", from uploaded JSON documents. This method checks the pool of
         * "virtual" annotations on the "virtual" component. Non-eponymous delegate
         * descriptors apparently don't have to do this.
         */
        if (_delegate == _component.getComponentClass()) {
            final Annotation bindingAnnotation =
                    _component.getAnnotation(bindingAnnotationType);
            if (bindingAnnotation != null) {
                return bindingAnnotation;
            }
        }

        for (Class<?> clazz : new BreadthFirstSupertypeIterable(_delegate)) {
            final Annotation bindingAnnotation =
                    clazz.getAnnotation(bindingAnnotationType);
            if (bindingAnnotation != null) {
                return bindingAnnotation;
            }
        }

        return null;

    }

    private void recordBindings(final Class<? extends ComponentInterface> iface) {

        final Bound bound = iface.getAnnotation(Bound.class);
        if (bound != null) {

            final Annotation bindingAnnotation = findNarrowestBindingAnnotation(bound
                    .value());

            if (bindingAnnotation != null) {
                _bindings.put(iface, bindingAnnotation);
            }
        }

        for (final Annotation a : iface.getDeclaredAnnotations()) {
            final Class<? extends Annotation> annotationType = a.annotationType();
            final ResourceBinding resourceBinding = annotationType.getAnnotation(ResourceBinding.class);
            if (resourceBinding != null) {
                _component.addResource(a, iface);
            }
        }

        for (final Class<?> superIface : iface.getInterfaces()) {
            if (ComponentInterface.class.isAssignableFrom(superIface)) {
                recordBindings(superIface.asSubclass(ComponentInterface.class));
            }

        }


    }

    private void registerFunction(Annotation annotation, Method method) {
        if (annotation.annotationType().isAnnotationPresent(Function.class)) {
            _component.addFunction(new DefaultFunctionDescriptor(this, method, annotation));
        }
    }

    @Override
    public void lifecycle(Class<? extends Annotation> lifecycle) throws Exception {
        if (lifecycle == PostLoad.class) {
            for (Map.Entry<Method, ScheduledTask> entry : _scheduled.entrySet()) {
                Method method = entry.getKey();
                Scheduled scheduled = method.getAnnotation(Scheduled.class);
                String taskName = _component.getIdentifier() + "/" + _delegate.getSimpleName() + "." + method.getName();
                ScheduledDelegateMethod op = new ScheduledDelegateMethod(method, scheduled);
                ScheduledTask task = Scheduler.getScheduler().schedule(op, taskName, scheduled.value());
                entry.setValue(task);
            }
        } else if (lifecycle == PreUnload.class) {
            for (ScheduledTask task : _scheduled.values()) {
                if (task != null) {
                    task.cancel();
                }
            }
        }
    }

    // yowza, this is expensive...
    private void addMessages(AnnotatedElement element) {
        for (Annotation annotation : element.getDeclaredAnnotations()) {
            addMessages(annotation);
        }
    }

    private void addMessages(Annotation annotation) {
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Class<?> type = method.getReturnType();
            try {
                if (method.isAnnotationPresent(MessageValue.class)) {
                    Object value = method.invoke(annotation);
                    if (value instanceof String) {
                        if (!StringUtils.isEmpty((String) value)) {
                            _component.addMessage((String) value);
                        }
                    } else {
                        for (String msg : (String[]) value) {
                            _component.addMessage(msg);
                        }
                    }
                } else if (Annotation.class.isAssignableFrom(type)) {
                    addMessages((Annotation) method.invoke(annotation));
                } else if (type.isArray() && Annotation.class.isAssignableFrom(type.getComponentType())) {
                    for (Annotation ann : ((Annotation[]) method.invoke(annotation))) {
                        addMessages(ann);
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Message exception", ex);
            }
        }
    }

    @Override
    public void addMethodRef(Method method) throws Exception {
        MethodRef ref = method.getAnnotation(MethodRef.class);
        String name = StringUtils.defaultIfEmpty(ref.value(), method.getName());
        // I genuinely don't want duplicates because if a subclass method overrides
        // a nonprivate superclass method, things could misbehave unpredictably.
        if (_methodRefs.containsKey(name)) {
            throw new Exception("Duplicate method ref: " + name);
        }
        _methodRefs.put(name, new DefaultFunctionDescriptor(this, method, ref));
    }

    private class ScheduledDelegateMethod implements Runnable {
        private final Method _method;
        private final DomainDTO _domain; // for a default-off component this will be /some/ domain
        private final Scheduled _scheduled;

        private ScheduledDelegateMethod(Method method, Scheduled scheduled) {
            _method = method;
            _method.setAccessible(true);
            _domain = Current.getDomainDTO();
            _scheduled = scheduled;
        }

        @Override
        public void run() {
            if (!_scheduled.singleton() || BaseServiceMeta.getServiceMeta().isDas()) {
                ManagedUtils.perform(new VoidOperation() {
                    @Override
                    public void execute() throws Exception {
                        final List<? extends Id> domains = !_scheduled.domain() ? Collections.singletonList(_domain)
                          : ServiceContext.getContext().getService(OverlordWebService.class).getAllDomains();
                        for (final Id domain : domains) {
                            ComponentEnvironment scriptEnv;
                            if (domain != null) {
                                scriptEnv = ServiceContext.getContext()
                                  .getService(DomainWebService.class).setupContext(domain.getId());
                            } else { // system-level task
                                scriptEnv = ComponentManager.initComponentEnvironment(ComponentCollection.NONE);
                            }
                            if (!scriptEnv.hasComponent(getComponent().getIdentifier()) ||
                              (scriptEnv.getComponent(getComponent().getIdentifier())) != getComponent()) {
                                // A script env rebuild may invalidate _me_
                                return;
                            }
                            Object object = scriptEnv.getSingletonCache().getRealService(BaseDelegateDescriptor.this);
                            _method.invoke(object);
                            Current.clearCache();
                        }
                    }
                });
            }
        }
    }
}
