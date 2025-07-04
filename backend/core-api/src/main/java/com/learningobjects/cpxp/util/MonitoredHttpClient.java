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

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.pool.PoolStats;

import java.util.HashMap;
import java.util.Map;

public class MonitoredHttpClient extends DefaultHttpClient {
    private String _name;

    public MonitoredHttpClient(String name, ClientConnectionManager conman, HttpParams params) {
        super(conman, params);
        _name = name;
        synchronized (__map) {
            __map.put(name, this);
        }
    }

    public String getName() {
        return _name;
    }

    public static void getStatus(Map<String, Object> status) {
        synchronized (__map) {
            for (MonitoredHttpClient client : __map.values()) {
                PoolingClientConnectionManager mgr = (PoolingClientConnectionManager) client.getConnectionManager();
                PoolStats stats = mgr.getTotalStats();
                status.put("httpclient." + client.getName() + ".Max", stats.getMax());
                status.put("httpclient." + client.getName() + ".Leased", stats.getLeased());
                status.put("httpclient." + client.getName() + ".Available", stats.getAvailable());
                status.put("httpclient." + client.getName() + ".Pending", stats.getPending());
            }
        }
    }

    private static final Map<String, MonitoredHttpClient> __map = new HashMap<String, MonitoredHttpClient>();
}

