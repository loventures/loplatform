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

import com.learningobjects.cpxp.component.annotation.Component;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class ComponentAnnotation implements Component {
    private String _name = "";
    private String _version = "";
    private String _description = "";
    private String _icon = "";
    private String[] _alias = {};
    private String _context = "*";
    private boolean _enabled = true;
    private Class<?>[] _dependencies = {};
    private Class<?>[] _suppresses = {};
    private boolean _i18n = false;
    private String _implementation = "";

    public ComponentAnnotation() {}

    public ComponentAnnotation(Component annotation) {
        _name = annotation.name();
        _version = annotation.version();
        _description = annotation.description();
        _icon = annotation.icon();
        _alias = annotation.alias();
        _context = annotation.context();
        _enabled = annotation.enabled();
        _dependencies = annotation.dependencies();
        _suppresses = annotation.suppresses();
        _i18n = annotation.i18n();
        _implementation = annotation.implementation();
    }

    public void setName(String name) { _name = name;}

    public void setVersion(String version) {_version = version;}

    public void setDescription(String description) {
        _description = description;
    }

    public void setIcon(String icon) {
        _icon = icon;
    }

    public void setAlias(String[] alias) {
        _alias = alias;
    }

    public void setContext(String context) {
        _context = context;
    }

    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public void setDependencies(Class<?>[] dependencies) {
        _dependencies = dependencies;
    }

    public void setSuppresses(Class<?>[] suppresses) {
        _suppresses = suppresses;
    }

    public void setI18n(boolean i18n) { _i18n = i18n;}

    // major hack
    public void setImplementations(String[] implementations) {
        _implementation = implementations[0];
    }

    public void setImplementation(String implementation) {
        _implementation = implementation;
    }

    @Override public String name() { return _name; }
    @Override public String version() { return _version; }
    @Override public String description() { return _description; }
    @Override public String icon() { return _icon; }
    @Override public String[] alias() { return _alias; }
    @Override public String context() { return _context; }
    @Override public boolean enabled() { return _enabled; }
    @Override public Class<?>[] dependencies() { return _dependencies; }
    @Override public Class<?>[] suppresses() { return _suppresses; }
    @Override public boolean i18n() { return _i18n; }
    @Override public String implementation() { return _implementation; }
    @Override public Class<Component> annotationType() { return Component.class; }
}
