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

package com.learningobjects.de;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import loi.cp.i18n.BundleMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BasicReport {

    private final List<BundleMessage> info;

    private final List<BundleMessage> warnings;

    private final List<BundleMessage> errors;

    public static BasicReport empty() {
        return new BasicReport(new ArrayList<>(),
          new ArrayList<>(), new ArrayList<>());
    }

    public static BasicReport fromErrors(final BundleMessage... errors) {
        return new BasicReport(Arrays.asList(errors), new ArrayList<>(), new ArrayList<>());
    }

    public static BasicReport fromErrors(final List<BundleMessage> errors) {
        return new BasicReport(new ArrayList<>(errors), new ArrayList<>(), new ArrayList<>());
    }

    /* for subclass use only */
    protected BasicReport() {
        info = new ArrayList<>();
        warnings = new ArrayList<>();
        errors = new ArrayList<>();
    }

    private BasicReport(
            final List<BundleMessage> errors, final List<BundleMessage> warnings, final List<BundleMessage> info) {
        this.errors = errors;
        this.warnings = warnings;
        this.info = info;
    }

    /**
     * Jonx for Jackson only. 'success' is a computed property but @JsonCreators have no
     * way to ignore them, so the arg is here but unused. Not Jackson clients should use
     * one of the factory methods. Even private clients should use {@link
     * #BasicReport(List, List, List)}
     */
    @JsonCreator
    private BasicReport(
            @JsonProperty("errors") final List<BundleMessage> errors,
            @JsonProperty("warnings") final List<BundleMessage> warnings,
            @JsonProperty("info") final List<BundleMessage> info,
            @JsonProperty("success") final boolean success) {
        this.errors = errors;
        this.warnings = warnings;
        this.info = info;
    }

    public void addInfo(final BundleMessage info) {
        this.info.add(info);
    }

    public void addError(final BundleMessage error) {
        errors.add(error);
    }

    public void addWarning(final BundleMessage warning) {
        warnings.add(warning);
    }

    public void addAll(final BasicReport report) {
        info.addAll(report.getInfo());
        errors.addAll(report.getErrors());
        warnings.addAll(report.getWarnings());
    }

    public List<BundleMessage> getInfo() {
        return List.copyOf(info);
    }

    public List<BundleMessage> getErrors() {
        return List.copyOf(errors);
    }

    public List<BundleMessage> getWarnings() {
        return List.copyOf(warnings);
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public boolean equals(final Object obj) {
        final boolean equals;

        if (this == obj) {
            equals = true;
        } else if (obj instanceof BasicReport) {
            final BasicReport that = (BasicReport) obj;
            equals = Objects.equals(this.info, that.info) &&
                    Objects.equals(this.errors, that.errors) &&
                    Objects.equals(this.warnings, that.warnings);
        } else {
            equals = false;
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, errors, warnings);
    }

    @Override
    public String toString() {
        return JacksonUtils.getMapper().valueToTree(this).toString();
    }
}
