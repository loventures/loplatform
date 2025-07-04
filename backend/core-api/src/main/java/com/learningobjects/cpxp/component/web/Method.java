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

package com.learningobjects.cpxp.component.web;

public enum Method {
    OPTIONS, HEAD, GET, POST, PUT, DELETE, PATCH, PROPFIND, Any, View;

    public boolean matches(String m) {
        switch (this) {
          case Any:
              return true;
          case View: // meh
              return "GET".equals(m) || "HEAD".equals(m) || "POST".equals(m);
          case GET:
              return "GET".equals(m) || "HEAD".equals(m);
          default:
              return m.equals(name());
        }
    }

    public String literal() {
        return isTerminal() ? name() : "GET";
    }

    public boolean isTerminal() {
        return (this != Any) && (this != View);
    }

    public boolean isUpdate() {
        return (this == POST) || (this == PUT) || (this == DELETE) || (this == PATCH);
    }
}
