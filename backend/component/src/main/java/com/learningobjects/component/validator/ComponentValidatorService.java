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

package com.learningobjects.component.validator;

import com.google.common.base.Function;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.validator.ComponentValidationException;
import com.learningobjects.cpxp.component.validator.ComponentValidator;
import com.learningobjects.cpxp.component.validator.Validated;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Services for providing support for component validation.
 */
@Service
public class ComponentValidatorService {

    /**
     * Validate the component with all the available relevant component validators.
     */
    public void validate(final ComponentInterface ci) throws ComponentValidationException {
        final Collection<ComponentValidator> validators = getComponentValidators(ci);
        for (ComponentValidator validator : validators) {
            validator.validate(ci);
        }
    }

    /**
     * Returns all the component validators for a specific component.
     */
    public Collection<ComponentValidator> getComponentValidators(final ComponentInterface ci) {

        final Function<Class<?>, ComponentValidator> toValidator = (Class<?> iface) -> {
            final Optional<Validated> validated = Optional.ofNullable(iface.getAnnotation(Validated.class));
            return validated.map(v -> v.by())
                            .filter(c -> ComponentSupport.getComponentDescriptor(c) != null)
                            .map(c -> ComponentSupport.get(c))
                            .orElse(null);
        };

        return BreadthFirstSupertypeIterable.from(ci.getClass())
                .filter(c -> ClassUtils.isAssignable(c, ComponentInterface.class))
                .map(toValidator)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
