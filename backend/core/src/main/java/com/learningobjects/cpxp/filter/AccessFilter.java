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

package com.learningobjects.cpxp.filter;

import com.learningobjects.cpxp.util.TomcatUtils;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ResponseFacade;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccessFilter extends AbstractFilter {
    private static final Logger logger = Logger.getLogger(AccessFilter.class.getName());

    private static Status _status = new Status();

    public AccessFilter() {
        super();
    }

    private static Counter responseBytesGauge = Counter
      .build()
      .name("cp_response_bytes_total")
      .help("total bytes transmitted")
      .register();
    private static Counter responseCountCounter = Counter
      .build()
      .name("cp_response_total")
      .help("the total number of responses")
      .register();
    private static Gauge responseAvgTimeGauge = Gauge
      .build()
      .name("cp_response_time_millis")
      .help("the average response time in millis")
      .register();
    private static Gauge responseSlaTimeGauge = Gauge
      .build()
      .name("cp_response_slatime_millis")
      .help("average sla time in millis")
      .register();
    private static Counter responseTypeGauge = Counter
      .build()
      .name("cp_response_type_total")
      .help("the total number of responses by class of response")
      .labelNames("class")
      .register();

    @Override
    protected void filterImpl(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, FilterChain chain)
            throws IOException, ServletException {

        long start = System.currentTimeMillis();

        chain.doFilter(httpRequest, httpResponse);

        long finish = System.currentTimeMillis();
        long elapsed = finish - start;

        if (!isStatusCheck(httpRequest)) {
            try {
                // this is clearly wedded to Tomcat but the other option, of wrapper
                // the response, runs afoul of some issue with request dispatching
                // and/or Shale RPC
                ResponseFacade facade = TomcatUtils.getResponseFacade(httpResponse);
                final int statusCode = facade.getStatus();
                final long bytes = facade.getContentWritten();

                // capture for the service layer; domain id will have been
                // filled in either by CurrentFilter or, in the case of
                // cached file-based media, SendFileFilter. If unset then
                // the domain is unknown so don't record the info.
                _status.recordResponse(statusCode, bytes, elapsed);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Access filter error", e);
            }
        }
    }

    public static boolean isStatusCheck(HttpServletRequest httpRequest) {
        String requestUri = httpRequest.getRequestURI();
        return ("/".equals(requestUri) && "HEAD".equals(httpRequest.getMethod())) || "/control/status".equals(requestUri) || "/control/mstatus".equals(requestUri);
    }

    public static void getStatus(Map<String,Object> map) {
        _status.getStatus(map);
    }

    public static final long SLA_LIMIT = 80L * 1024;

    private static class Status {
        private Map<Integer, Long> _requestsByCode = new HashMap<Integer, Long>();
        private long _bytes, _time, _count;
        private RollingAverage _responseTime = new RollingAverage();
        private RollingAverage _slaTime = new RollingAverage();

        public synchronized void recordResponse(int statusCode, long bytes, long time) {
            increment(_requestsByCode, statusCode / 100);
            _bytes += bytes;
            _time += time;
            ++ _count;
            _responseTime.recordTime(time);
            if (bytes < SLA_LIMIT) {
                _slaTime.recordTime(time);
                responseSlaTimeGauge.set(time);
            }

            responseCountCounter.inc();
            responseBytesGauge.inc(bytes);
            responseTypeGauge.labels((statusCode / 100 + "xx")).inc();
            responseAvgTimeGauge.set(_responseTime.getAverage());
        }

        private static <T> void increment(Map<T, Long> map, T key) {
            Long value = map.get(key);
            value = 1L + ((value == null) ? 0 : value.longValue());
            map.put(key, value);
        }

        public synchronized void getStatus(Map<String, Object> statusMap) {
            for (Map.Entry<Integer, Long> entry : _requestsByCode.entrySet()) {
                statusMap.put("cp.ResponseCode." + entry.getKey() + "xx", entry.getValue());
            }
            statusMap.put("cp.Response.Count", _count);
            statusMap.put("cp.Response.Bytes", _bytes);
            statusMap.put("cp.Response.Time", _time);
            statusMap.put("cp.Response.ResponseTime", _responseTime.getAverage());
            statusMap.put("cp.Response.SlaTime", _slaTime.getAverage());
        }
    }

    private static class RollingAverage {
        private static final int BUCKETS = 5;
        private static final long MINUTE = 60000L;
        private final long[] _time = new long[BUCKETS];
        private final long[] _count = new long[BUCKETS];
        private int _i;

        public void recordTime(long time) {
            int i = (int) ((System.currentTimeMillis() / MINUTE) % BUCKETS); // pick a bucket
            if (i != _i) { // if I roll into a new bucket, clear it
                // this will have the approximate effect of giving me
                // a 5 minute rolling average.
                _i = i;
                _time[i] = time;
                _count[i] = 1L;
            } else {
                _time[i] += time;
                _count[i] += 1L;
            }
        }

        public long getAverage() {
            long num = 0L, den = 0L;
            for (long time : _time) num += time;
            for (long count : _count) den += count;
            return (den > 0L) ? num / den : 0L;
        }
    }

}
