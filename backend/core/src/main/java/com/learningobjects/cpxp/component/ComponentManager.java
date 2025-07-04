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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.annotation.PostLoad;
import com.learningobjects.cpxp.component.annotation.PreShutdown;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.language.LanguageService;
import com.learningobjects.cpxp.util.ParallelStartup;
import com.learningobjects.cpxp.util.message.MessageMap;
import com.learningobjects.cpxp.util.message.MessageMapCompositor;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ComponentManager  {
    private static final Logger logger = Logger.getLogger(ComponentManager.class.getName());
    private static final ComponentManager INSTANCE = new ComponentManager();

    private ComponentRing _ring0 = ClassLoaderRing.Ring0();

    private ComponentManager() {
    }

    public static ComponentRing getComponentRing() {
        return INSTANCE._ring0;
    }

    public static ComponentEnvironment initComponentEnvironment(ComponentCollection collection) {
        return INSTANCE.getEnvironment(collection, true);
    }

    public static ComponentEnvironment getComponentEnvironment() {
        return INSTANCE.getEnvironment(ComponentCollection.NONE, false);
    }

    public static ComponentEnvironment getComponentEnvironment(ComponentCollection collection) {
        return INSTANCE.getEnvironment(collection, false);
    }

    public static void shutdown() {
        INSTANCE.shutdown0();
    }

    private final Map<String, ComponentEnvironment> _environments = new HashMap<>();
    private final Set<ComponentDescriptor> _loaded = Collections.synchronizedSet(new HashSet<ComponentDescriptor>());

    private ComponentEnvironment getEnvironment(ComponentCollection collection, boolean init) {
        ComponentEnvironment environment;
        // The synchronization logic here is to allow quick read access from the
        // shared map; if an env rebuild is required then I fall out with an
        // uninitialized environment and then synchronize on that to ensure that
        // only one thread actually builds it...
        synchronized (_environments) {
            environment = _environments.get(collection.getIdentifier());
            if ((environment == null) || (init && (environment.getCollection().getLastModified() != collection.getLastModified()))) {
                collection.init();
                environment = new BaseComponentEnvironment(collection, environment);
                _environments.put(collection.getIdentifier(), environment);
            }
        }
        boolean stale = environment.load();
        /* XXX: this seems weird, but otherwise there's no way for us to get at
         * the component classpath from places that can't see ComponentSupport. */
        Thread.currentThread().setContextClassLoader(environment.getClassLoader());
        if (init) {
            Current.put(ComponentRing.class, environment.getRing());
            BaseWebContext.getContext().setComponentEnvironment(environment);
            // This hardly belongs here, but tangled webs
            try {
                LanguageService languageService = ComponentSupport.lookupService(environment, LanguageService.class);
                MessageMapCompositor messageMapCompositor = languageService.getDomainMessages();
                MessageMap messages = messageMapCompositor.getCompositeMap(null); // the default
                BaseWebContext.getContext().initMessages(messages);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error initializing messages", e);
            }
        }
        if (stale) {
            final ComponentEnvironment env = environment;
            ParallelStartup.foreach(environment.getComponents(), cd -> {
                if (_loaded.add(cd)) {
                    Current.put(BaseComponentEnvironment.class, env);
                    BaseWebContext.getContext().setComponentEnvironment(env); // ugh .. boy really ..
                    lifecycle(env, cd, PostLoad.class);
                }
            });
        }
        return environment;
    }

    private void shutdown0() {
        lifecycle(getComponentEnvironment(), getComponentEnvironment().getComponents(), PreShutdown.class);
    }

    private void lifecycle(ComponentEnvironment environment, Iterable<ComponentDescriptor> components, Class<? extends Annotation> phase) {
        Map<String, ComponentDescriptor> map = new TreeMap<>(); // tree map for some stability
        for (ComponentDescriptor component : components) {
            map.put(component.getIdentifier(), component);
        }
        for (ComponentDescriptor component : components) {
            lifecycle(environment, component.getIdentifier(), map, phase);
        }
    }

    private void lifecycle(ComponentEnvironment environment, String id, Map<String, ComponentDescriptor> map, Class<? extends Annotation> phase) {
        ComponentDescriptor component = map.remove(id);
        if (component != null) {
            // bootstrap any dependencies first, ignoring loops...
            for (Class<?> dep : component.getComponentAnnotation().dependencies()) {
                lifecycle(environment, dep.getName(), map, phase);
            }
            lifecycle(environment, component, phase);
        }
    }

    private void lifecycle(ComponentEnvironment environment, ComponentDescriptor component, Class<? extends Annotation> phase) {
       try {
           ComponentSupport.lifecycle(component.getInstance(environment, null, null), phase);
       } catch (Throwable th) {
           logger.log(Level.WARNING, "Error running " + phase.getSimpleName() + ": " + component.getIdentifier(), th);
       }
    }

    private static final AtomicBoolean __started = new AtomicBoolean(false);

    public static void awaitStartup() throws InterruptedException {
        synchronized (__started) {
            while (!__started.get()) {
                __started.wait();
            }
        }
    }

    public static void startup() {
        initComponentEnvironment(ComponentCollection.NONE);
        synchronized (__started) {
            __started.set(true);
            __started.notifyAll();
        }
    }
}
