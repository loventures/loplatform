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

import org.apache.pekko.actor.ActorSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.WebContext;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem;
import com.learningobjects.cpxp.scala.concurrent.CpxpExecutionContext;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.session.SessionDTO;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.util.DaemonThreadFactory;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.cache.Cache;
import com.learningobjects.de.authorization.SecurityContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.HttpClient;
import org.hibernate.Session;
import scala.collection.Seq;
import scala.concurrent.ExecutionContext;
import scaloi.misc.TimeSource;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A DI evaluator that infers the value to be injected based solely on its type. This
 * supports DI based on globally-registered type-value bindings as well as
 * thread-local DI where the type is looked up in {Current.get(Class<T>)} and if a
 * value is returned, that is injected. That mechanism is abhorrent and deprecated
 * but rampant.
 */
@Evaluates(Infer.class)
public class InferEvaluator extends AbstractEvaluator {
    private boolean _cache;

    @Override
    protected void init() {
        _cache = (_subtype != null) && Cache.class.isAssignableFrom(_subtype);
    }

    @Override
    protected Object getValue(ComponentInstance instance, Object object) {
        if (_cache) {
            return inferCache();
        }

        Optional<Object> byInferrer =  orElse(findType(__inferrers.stream(), _subtype), () -> findType(__inferrers.stream(), _type))
          .map(Pair::getRight)
          .map(inf -> {
              try {
                  return inf.infer(_delegate, instance);
              } catch (Exception e) {
                  throw new RuntimeException(e);
              }
          });

        // TODO: I don't like the use of current but it's hard otherwise for AbstractDescriptor
        // to pass annotations into enforcers unless I use the component instance. Hrm.
        Supplier<Object> byCurrent = () -> Optional.ofNullable(Current.get(Infer.class))
          .filter(o -> _subtype.isInstance(o))
          .orElseGet(() -> Current.get(_subtype));

        return byInferrer.orElseGet(byCurrent);
    }

    @SuppressWarnings("unchecked")
    private Cache<?, ?, ?> inferCache() {
        final Class<Cache<? extends Serializable, ?, ?>> cacheClass = (Class) _subtype;
        return CacheInjector.getCache(cacheClass);
    }

    private Optional<Pair<Type, Inferrer<?>>> findType(Stream<Pair<Type, Inferrer<?>>> inferrers, Type t) {
        return inferrers.filter(p -> p.getLeft().equals(t)).findFirst();
    }

    @Override
    public boolean isStateless() {
        return __statelessInferables.stream().anyMatch(t -> TypeUtils.equals(t, _type)) || _cache;
    }

    private static <T> Optional<T> orElse(Optional<T> opt, Supplier<Optional<T>> other) {
        if (opt.isPresent()) {
            return opt;
        } else {
            return other.get();
        }
    }

    private static final List<Pair<Type, Inferrer<?>>> __inferrers = new ArrayList<>();
    private static final Set<Type> __statelessInferables = new HashSet<>();
    private static final ExecutorService __es =
      Executors.newFixedThreadPool(
        Math.max(4, Runtime.getRuntime().availableProcessors()),
        new DaemonThreadFactory());

    /**
     * XXX: Not Thread safe!
     */
    private static <T> void registerInferrer(Type clas, Inferrer<T> inferrer, boolean stateful) {
        __inferrers.removeIf(p -> p.getLeft().equals(clas));
        __inferrers.add(new ImmutablePair<>(clas, inferrer));
        if (!stateful) {
            __statelessInferables.add(clas);
        }
    }

    // Can only externally regester a stateless type
    public static <T> void registerInferrer(Type clas, Inferrer<T> inferrer) {
        registerInferrer(clas, inferrer, false);
    }

    static Optional<Boolean> isStatelessInferable(Type clas) {
        /* Type#equals is mildly wrongo */
        if (__inferrers.stream().anyMatch(ti -> ti.getLeft().equals(clas))) {
            return Optional.of(__statelessInferables.contains(clas));
        } else {
            return Optional.empty(); // a.k.a. FILE_NOT_FOUND
        }
    }

    @FunctionalInterface
    public interface Inferrer<T> {
        T infer(DelegateDescriptor delegate, ComponentInstance instance);
    }

    static {
        /* stateful */
        registerInferrer(WebContext.class, (delegate, instance) -> BaseWebContext.getContext(), true);
        registerInferrer(HtmlWriter.class, ((delegate, instance) -> BaseWebContext.getContext().getHtmlWriter()), true);
        registerInferrer(HttpServletRequest.class, (delegate, instance) -> BaseWebContext.getContext().getRequest(), true);
        registerInferrer(Method.class, (delegate, instance) -> Method.valueOf(BaseWebContext.getContext().getRequest().getMethod()), true);
        registerInferrer(HttpServletResponse.class, (delegate, instance) -> BaseWebContext.getContext().getResponse(), true);
        registerInferrer(DomainDTO.class, (delegate, instance) -> Current.getDomainDTO(), true);
        registerInferrer(UserDTO.class, (delegate, instance) -> Current.getUserDTO(), true);
        registerInferrer(SessionDTO.class, (delegate, instance) -> SessionDTO.apply(Current.getSessionPk(), Current.getSessionId()), true);
        registerInferrer(Date.class, (delegate, instance) -> Optional.ofNullable(Current.getTime()).orElse(new Date()), true);
        registerInferrer(Timestamp.class, (delegate, instance) -> new Timestamp(Optional.ofNullable(Current.getTime()).orElse(new Date()).getTime()), true);
        registerInferrer(LocalDateTime.class, (delegate, instance) -> new Timestamp(Optional.ofNullable(Current.getTime()).orElse(new Date()).getTime()).toLocalDateTime(), true);
        registerInferrer(TimeZone.class, (delegate, instance) -> Optional.ofNullable(Current.getDomainDTO()).map(InferEvaluator::domainTZ).orElse(TimeZone.getDefault()), true);
        registerInferrer(EntityContext.class, ((delegate, instance) -> ManagedUtils.getEntityContext()), true);
        registerInferrer(EntityManager.class, ((delegate, instance) -> ManagedUtils.getEntityContext().getEntityManager()), true);
        registerInferrer(ExecutionContext.class, (delegate, instance) -> CpxpExecutionContext.create(getEnvironment(instance)), true);
        registerInferrer(Session.class, ((delegate, instance) -> ManagedUtils.getEntityContext().getEntityManager().unwrap(Session.class)), true);
        registerInferrer(SecurityContext.class, ((delegate, instance) -> Current.getTypeSafe(SecurityContext.class)), true);

        /* stateless */
        registerInferrer(ComponentDescriptor.class, (delegate, instance) ->
          (instance != null) ? instance.getComponent() : delegate.getComponent());
        registerInferrer(ComponentEnvironment.class, (delegate, instance) -> getEnvironment(instance));
        registerInferrer(DelegateDescriptor.class, (delegate, instance) -> delegate);
        registerInferrer(ComponentInstance.class, (delegate, instance) -> instance);
        registerInferrer(ServiceMeta.class, (delegate, instance) -> BaseServiceMeta.getServiceMeta());
        registerInferrer(ServletContext.class, (delegate, instance) -> BaseWebContext.getServletContext());
        registerInferrer(ActorSystem.class, (delegate, instance) -> CpxpActorSystem.system());
        registerInferrer(ObjectMapper.class, (delegate, instance) -> ComponentSupport.getObjectMapper());
        registerInferrer(ExecutorService.class, (delegate, instance) -> __es);
        registerInferrer(HttpClient.class, (delegate, instance) -> HttpUtils.getHttpClient());
        registerInferrer(Ontology.class, (delegate, instance) -> BaseOntology.getOntology());
        registerInferrer(TimeSource.class, (delegate, instance) -> (TimeSource) () -> Optional.ofNullable(Current.getTime()).orElse(new Date()).getTime());

        final Type wild = TypeUtils.wildcardType().build(); // häcky häcky phtangh
        registerInferrer(TypeUtils.parameterize(Seq.class, TypeUtils.parameterize(Cache.class, wild, wild, wild)), // wild, wild, wild
          (delegate, instance) -> CacheInjector.caches());
    }

    /** Either the given instance's bound component environment, or the ambient thread-local one. */
    private static ComponentEnvironment getEnvironment(ComponentInstance instance) {
        return (instance != null && instance.getEnvironment() != null)
          ? instance.getEnvironment()
          : BaseWebContext.getContext().getComponentEnvironment();
    }

    private static TimeZone domainTZ(DomainDTO dto) {
        return TimeZone.getTimeZone(dto.timeZoneId());
    }
}
