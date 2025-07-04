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

@GenericGenerator(name = "cpxp-sequence",
        parameters = { @Parameter(name = "optimizer", value = "com.learningobjects.cpxp.service.CpxpOptimizer"),
                       @Parameter(name = "sequence_name", value = "cpxp_sequence"),
                       @Parameter(name = "initial_value", value = "10000000"),
                       @Parameter(name = "increment_size", value = "100") },
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator")
package com.learningobjects.cpxp.service;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
