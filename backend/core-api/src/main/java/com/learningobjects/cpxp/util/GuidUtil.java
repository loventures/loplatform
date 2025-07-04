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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.net.InetAddress;
import java.util.Date;

import com.learningobjects.cpxp.ServiceMeta;
import org.apache.commons.lang3.StringUtils;

public abstract class GuidUtil {
    public static String errorGuid() {
        String rndPart = StringUtils.leftPad(NumberUtils.toBase36Encoding(BigInteger.valueOf(NumberUtils.getSecureRandom().nextInt(1679616))), 4, '0');
        String timePart = NumberUtils.toBase36Encoding(BigInteger.valueOf(System.currentTimeMillis()));
        return rndPart + "-" + timePart + "-" + getHostPart();
    }

    public static String shortGuid() {
        return StringUtils.leftPad(NumberUtils.toBase36Encoding(BigInteger.valueOf(NumberUtils.getSecureRandom().nextInt(1679616))), 4, '0');
    }

    public static String mediumGuid() {
        return StringUtils.leftPad(NumberUtils.toBase36Encoding(BigInteger.valueOf(Math.abs(NumberUtils.getSecureRandom().nextLong()) % 2821109907456L)), 8, '0');
    }

    public static String temporalGuid(Date date) {
        long value = (date == null) ? System.currentTimeMillis() : date.getTime();
        String timePart = StringUtils.reverse(NumberUtils.toBase36Encoding(BigInteger.valueOf(value)));
        String rndPart = StringUtils.leftPad(NumberUtils.toBase36Encoding(BigInteger.valueOf(NumberUtils.getSecureRandom().nextInt(1679616))), 4, '0');
        return timePart + "/" + rndPart;
    }

    public static Date decodeTemporalGuid(String str) {
        return new Date(NumberUtils.fromBase36Encoding(StringUtils.reverse(StringUtils.substringBefore(str, "/"))).longValue());
    }

    public static String guid() {
        String rndPart = StringUtils.leftPad(NumberUtils.toBase36Encoding(BigInteger.valueOf(NumberUtils.getSecureRandom().nextInt(1679616))), 4, '0');
        String timePart = NumberUtils.toBase36Encoding(BigInteger.valueOf(System.currentTimeMillis()));
        return rndPart + "-" + timePart;
    }

    public static String longGuid() {
        String rndPart = StringUtils.leftPad(NumberUtils.toBase36Encoding(BigInteger.valueOf(Math.abs(NumberUtils.getSecureRandom().nextLong()) % 2821109907456L)), 8, '0');
        String timePart = NumberUtils.toBase36Encoding(BigInteger.valueOf(System.currentTimeMillis()));
        return rndPart + "-" + timePart;
    }

    public static String deleteGuid(Date date, Long userId) {
        return GuidUtil.temporalGuid(date) + "/" + userId;
    }

    public static String getErrorHost(String guid) {
        return StringUtils.substringAfter(StringUtils.substringAfter(guid, "-"), "-");
    }

    public static long getErrorTime(String guid) {
        String timePart = StringUtils.substringBefore(StringUtils.substringAfter(guid, "-"), "-");
        return NumberUtils.fromBase36Encoding(timePart).longValue();
    }

    private static String __hostPart;
    private static String __clusterName;

    public synchronized static void configure(String clusterName) {
        __clusterName = clusterName;
    }

    public synchronized static String getHostPart() {
        if (__hostPart == null) {
            try {

                InetAddress localhost = InetAddress.getLocalHost();
                String hostAddress = localhost.getHostAddress();
                String hostName = localhost.getHostName();
                if (__clusterName != null) {
                    __hostPart = __clusterName ;
                } else if (hostName.equals(hostAddress)) { // 10.0.0.99 -> 99
                    int index0 = hostAddress.lastIndexOf('.'); // IPv4
                    int index1 = hostAddress.lastIndexOf(':'); // IPv6
                    int index = Math.max(index0, index1);
                    __hostPart = hostAddress.substring(1 + index);
                } else { // server1.loi.com -> server1
                    __hostPart = StringUtils.substringBefore(hostName, ".");
                }
                __hostPart = __hostPart.toLowerCase();
            } catch (Exception ex) {
                __hostPart = "unknown";
            }
        }
        return __hostPart;
    }

}
