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

package loi.cp.web.handler.impl;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.net.URLUtils;

import java.util.*;

/**
 * Parses embed routes from an embed expression. The embed route can end up having
 * embeds itself. For example, {@code questions.taxons} is parsed into the route
 * {@code questions;embed=taxons}.
 *
 * <p>
 *     CMA-133. This factorizes the routes. {@code questions.taxons,questions.content}
 *     will factorize to questions;embed=taxons,content
 * </p>
 */
public class EmbedRouteParser {

    public List<String> parseRoutes(final String embedValue) {

        if (StringUtils.isEmpty(embedValue)) {
            return Collections.emptyList();
        }

        /*
         * not sure if these var names are mathematically correct but in math you could
         * factorize (ax+ay) to a(x+y). In this method, the "a" is a coefficient. The
         * (x+y) is a factor
         */
        final Set<String> coefficients = new LinkedHashSet<>();
        final ListMultimap<String, String> factorsByCoefficient =
                ArrayListMultimap.create();

        final Iterable<String> expressions = Splitter.on(',').split(embedValue);
        for (final String expression : expressions) {

            final String coeff = StringUtils.substringBefore(expression, ".");
            final String factor = StringUtils.substringAfter(expression, ".");

            if (isDivisible(expression)) {
                factorsByCoefficient.put(coeff, factor);
            }

            coefficients.add(coeff);

        }

        return getFactorizedRoutes(coefficients, factorsByCoefficient);

    }

    private List<String> getFactorizedRoutes(
            final Set<String> coefficients,
            final ListMultimap<String, String> factorsByCoefficient) {

        final List<String> factorized = new ArrayList<>();
        for (final String coefficient : coefficients) {

            final List<String> factors = factorsByCoefficient.get(coefficient);
            final String subEmbedParam = getSubEmbedParam(factors);
            final String value = URLUtils.decode(coefficient) + subEmbedParam;
            factorized.add(value);
        }

        return factorized;
    }

    private String getSubEmbedParam(final List<String> terms) {
        if (terms.isEmpty()) {
            return StringUtils.EMPTY;
        } else {
            return ";embed=" + Joiner.on(',').join(terms);
        }
    }

    private boolean isDivisible(final String expression) {
        return StringUtils.contains(expression, ".");
    }

}
