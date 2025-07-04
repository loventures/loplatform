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

package loi.cp.ldap;

class LdapUtils {
    static String escapeDN(String name) {
        StringBuilder sb = new StringBuilder();
        if ((name.length() > 0) && ((name.charAt(0) == ' ') || (name.charAt(0) == '#'))) {
            sb.append('\\'); // add the leading backslash if needed
        }
        for (int i = 0; i < name.length(); i++) {
            char curChar = name.charAt(i);
            switch (curChar) {
              case '\\':
                  sb.append("\\\\");
                  break;
              case ',':
                  sb.append("\\,");
                  break;
              case '+':
                  sb.append("\\+");
                  break;
              case '"':
                  sb.append("\\\"");
                  break;
              case '<':
                  sb.append("\\<");
                  break;
              case '>':
                  sb.append("\\>");
                  break;
              case ';':
                  sb.append("\\;");
                  break;
              default:
                  sb.append(curChar);
            }
        }
        if ((name.length() > 1) && (name.charAt(name.length() - 1) == ' ')) {
            sb.insert(sb.length() - 1, '\\'); // add the trailing backslash if needed
        }
        return sb.toString();
    }

    static String escapeLDAPSearchFilter(String filter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filter.length(); i++) {
            char curChar = filter.charAt(i);
            switch (curChar) {
              case '\\':
                  sb.append("\\5c");
                  break;
              case '*':
                  sb.append("\\2a");
                  break;
              case '(':
                  sb.append("\\28");
                  break;
              case ')':
                  sb.append("\\29");
                  break;
              case '\u0000':
                  sb.append("\\00");
                  break;
              default:
                  sb.append(curChar);
            }
        }
        return sb.toString();
    }
}
