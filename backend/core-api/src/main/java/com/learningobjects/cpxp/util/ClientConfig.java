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

package com.learningobjects.cpxp.util;

import com.typesafe.config.Config;

public class ClientConfig {
    public String socksHost;
    public int socksPort;

    public int maxConnections;
    public int connectionTimeout;
    public int soTimeout;

    public ClientConfig(String socksHost, int socksPort, int maxConnections, int connectionTimeout, int soTimeout) {
        this.socksHost = socksHost;
        this.socksPort = socksPort;
        this.maxConnections = maxConnections;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
    }

    public ClientConfig setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public ClientConfig setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
        return this;
    }

    public static ClientConfig fromConfig(Config config) {
        Config socksConfig = config.getConfig("com.learningobjects.cpxp.socks");
        Config httpConfig = config.getConfig("com.learningobjects.cpxp.httpclient");
        return new ClientConfig(
          socksConfig.getString("proxyHost"),
          socksConfig.getInt("proxyPort"),
          httpConfig.getInt("maxConnections"),
          httpConfig.getInt("connectionTimeout"),
          httpConfig.getInt("socketTimeout")
        );
    }
}
