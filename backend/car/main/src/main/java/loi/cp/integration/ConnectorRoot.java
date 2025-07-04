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

package loi.cp.integration;

import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.query.*;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.integration.IntegrationConstants;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;
import scaloi.GetOrCreate;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ConnectorRoot extends AbstractComponent implements ConnectorRootComponent {

    @Infer
    private ComponentEnvironment _environment;

    @Inject
    private FacadeService _facadeService;

    @Override
    public ApiQueryResults<SystemComponent> getSystems(ApiQuery query) {
        QueryBuilder qb = parent().querySystems();
        return ApiQuerySupport.query(qb, query, SystemComponent.class);
    }

    @Override
    public Optional<SystemComponent> getSystem(Long id) {
        return Optional.ofNullable(getSystems(ApiQuery.byId(id)).toOptional().orElse(null));
    }

    @Override
    public  ApiQueryResults<SystemName> getSystemNames(ApiQuery query0) {
        ApiQuery query = new ApiQuery.Builder(query0).addPropertyMappings(SystemComponent.class).build();
        ApiQueryResults<SystemComponent> results = getSystems(query);
        List<SystemName> systemNames = results.stream().map(SystemName::apply).collect(Collectors.toList());
        return new ApiQueryResults(systemNames, results.getFilterCount(), results.getTotalCount());
    }

    @Override
    public Optional<SystemName> getSystemName(Long id) {
        return getSystem(id).map(SystemName::apply);
    }

    @Override
    public <T extends SystemComponent> T addSystem(T system) {
        // TODO: SRS validation
        if (StringUtils.isBlank(system.getSystemId())) {
            throw new ValidationException("systemId", system.getSystemId(), "System id is required");
        } else if (StringUtils.isBlank(system.getName())) {
            throw new ValidationException("name", system.getSystemId(), "Name is required");
        }
        GetOrCreate<T> t = parent().getOrCreateSystemBySystemId(system.getSystemId(), system);
        if (t.isGotten()) {
            throw new ValidationException("systemId", system.getSystemId(), "Duplicate system id");
        }
        return t.result();
    }

    @Override
    public ApiQueryResults<SystemComponent> getLtiSystems(ApiQuery query) {
        ApiFilter ltiFilter = new BaseApiFilter(SystemComponent.PROPERTY_IMPLEMENTATION, PredicateOperator.LESS_THAN_OR_EQUALS, LtiSystemComponent.class.getName());
        return getSystems(new ApiQuery.Builder(query).addPrefilter(ltiFilter).build());
    }

    @Override
    public Optional<SystemComponent> getLtiSystem(Long id) {
        return Optional.ofNullable(getLtiSystems(ApiQuery.byId(id)).toOptional().orElse(null));
    }

    @Override
    public String generateKey() {
        String firstPart = StringUtils.leftPad(NumberUtils.toBase31Encoding(BigInteger.valueOf(Math.abs(NumberUtils.getSecureRandom().nextLong()) % 923521L)), 4, '2');
        String midPart = StringUtils.leftPad(NumberUtils.toBase31Encoding(BigInteger.valueOf(Math.abs(NumberUtils.getSecureRandom().nextLong()) % 852891037441L)), 8, '2');
        String lastPart = StringUtils.leftPad(NumberUtils.toBase31Encoding(BigInteger.valueOf(Math.abs(NumberUtils.getSecureRandom().nextLong()) % 923521L)), 4, '2');
        return firstPart + '-' + midPart + '-' + lastPart;
    }

    public SystemParentFacade parent() {
        return _facadeService.getFacade(IntegrationConstants.FOLDER_ID_SYSTEMS, SystemParentFacade.class);
    }

    public Optional<ConnectorConfig> getConnectorConfig(String identifier) {
        ComponentDescriptor cd = ComponentSupport.getComponentDescriptor(identifier);
        if (cd == null) {
            return Optional.empty();
        }
        ComponentInstance ci = cd.getInstance(_environment, null, null);
        Optional<Schema> schemaAnnotation = ComponentUtils.findAnnotation(ci.getInstance(SystemComponent.class), Schema.class);
        ConnectorConfig cf = new ConnectorConfig();
        cf.name = ci.getName();
        //TODO: Use functional combinators with Optional instead, filter/map instead of isPresent/value
        if (schemaAnnotation.isPresent() && !"connector".equals(schemaAnnotation.get().value())) {
            cf.schema = schemaAnnotation.get().value();
            cf.configs = cd.getConfigurations()
              .values()
              .stream()
              .sorted(Comparator.<ConfigurationDescriptor>comparingInt(o -> o.getConfiguration().order()))
              .map(cfd -> {
                ConfigEntry entry = new ConfigEntry();
                entry.id = cfd.getName();
                entry.name = (String) ci.evalI18n(cfd.getConfiguration().label());
                entry.type = cfd.getType();
                int size = cfd.getConfiguration().size();
                entry.size = (size < 0) ? 48 : size;
                return entry;
              })
              .collect(Collectors.toList());
        }
        return Optional.of(cf);
    }
}
