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

import com.learningobjects.cpxp.component.annotation.Archive;

public class ArchiveAnnotation implements Archive {
    private String _name = "";
    private String _version = "";
    private String _branch = "";
    private String _revision = "";
    private String _buildNumber = "";
    private String _buildDate = "";
    private boolean _available = true;
    private boolean _proxies = true;
    private String[] _dependencies = {};
    private Class<?>[] _suppresses = {};
    private String _implementing = "";

    public void setName(String name) {
        _name = name;
    }

    public void setVersion(String version) {
        _version = version;
    }

    public void setBranch(String branch) {
        _branch = branch;
    }

    public void setRevision(String revision) {
        _revision = revision;
    }

    public void setBuildNumber(String buildNumber) {
        _buildNumber = buildNumber;
    }

    public void setBuildDate(String buildDate) {
        _buildDate = buildDate;
    }

    public void setAvailable(boolean available) { _available = available; }

    public void setProxies(boolean proxies) {
        _proxies = proxies;
    }

    public void setDependencies(String[] dependencies) {
        _dependencies = dependencies;
    }

    public void setSuppresses(Class<?>[] suppresses) {
        _suppresses = suppresses;
    }

    public void setImplementing(String implementing) { _implementing = implementing; }

    @Override public String name() { return _name; }
    @Override public String version() { return _version; }
    @Override public String branch() { return _branch; }
    @Override public String revision() { return _revision; }
    @Override public String buildNumber() { return _buildNumber; }
    @Override public String buildDate() { return _buildDate; }
    @Override public boolean available() { return _available; }
    @Override public boolean proxies() { return _proxies; }
    @Override public String[] dependencies() { return _dependencies; }
    @Override public Class<?>[] suppresses() { return _suppresses; }
    @Override public Class<Archive> annotationType() { return Archive.class; }
    @Override public String implementing() { return _implementing; }
}
