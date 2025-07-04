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

package com.learningobjects.cpxp.service.startup;

/**
 * Constants that define how startup task status is persisted to the database.
 */
public interface StartupTaskConstants {
    String DATA_TYPE_STARTUP_TASK_IDENTIFIER = "StartupTask.identifier";

    String DATA_TYPE_STARTUP_TASK_VERSION = "StartupTask.version";

    String DATA_TYPE_STARTUP_TASK_TIMESTAMP = "StartupTask.timestamp";

    String DATA_TYPE_STARTUP_TASK_STATE = "StartupTask.state";

    String DATA_TYPE_STARTUP_TASK_LOGS = "StartupTask.logs";

    String ITEM_TYPE_STARTUP_TASK = "StartupTask";
}
