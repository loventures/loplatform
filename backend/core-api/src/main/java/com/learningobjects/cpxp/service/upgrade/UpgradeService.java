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

package com.learningobjects.cpxp.service.upgrade;

import javax.ejb.Local;
import java.util.List;

/**
 * A service for monitoring and controlling versioned upgrades.
 */
@Local
public interface UpgradeService {
    public void initDomains();

    public String acquireCentralHost(String myself);

    public List<SystemInfo> findRecentHosts();

    public void heartbeat(String myself, String name);

    public void releaseCentralHost(String myself);
}
