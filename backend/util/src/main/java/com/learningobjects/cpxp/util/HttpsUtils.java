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

import javax.net.ssl.*;

public class HttpsUtils {

    /**
     * For development, set up the SSL support to accept self signed
     * certificates.
     */
    public static void setLenientCertPolicy() {
        // do NOT use in production, EVER
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                    /*
                    System.out.println("authType is " + authType);
                    System.out.println("cert issuers");
                    for (int i = 0; i < certs.length; i++) {
                        System.out.println("\t"
                                + certs[i].getIssuerX500Principal().getName());
                        System.out.println("\t" + certs[i].getIssuerDN().getName());
                    }
                    */
                }
            } };

        // on some dev systems, even connecting by IP address causes a hostname verification failure
        HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
                        /*
                        System.out.println("Warning: URL Host: "+urlHostName+" vs. "+session.getPeerHost());
                        */
                    }
                    return true;
                }
            };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            System.exit(1);
        }
    }

}
