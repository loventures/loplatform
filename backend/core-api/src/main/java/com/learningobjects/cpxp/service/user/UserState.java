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

package com.learningobjects.cpxp.service.user;

/**
 * User state and 'disabled' are duplicative, but this offers the least invasive change.
 */
public enum UserState {
    Pending(false, false), // pending administrator approval
    Unconfirmed(false, false), // pending user email address confirmation
    Active(false, true),
    Suspended(true, false),
    Gdpr(true, false);

    private boolean _disabled;
    private boolean _inDirectory;

    UserState(boolean disabled, boolean inDirectory) {
        _disabled = disabled;
        _inDirectory = inDirectory;
    }

    public boolean getDisabled() {
        return _disabled;
    }

    public boolean getInDirectory() {
        return _inDirectory;
    }
}
