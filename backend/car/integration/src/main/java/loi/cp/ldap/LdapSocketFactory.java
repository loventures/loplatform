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

import com.typesafe.config.Config;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSSLSocketFactoryExt;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

class LdapSocketFactory implements LDAPSSLSocketFactoryExt {
    private static Logger logger = Logger
            .getLogger(LdapSocketFactory.class.getName());
    private SSLSocketFactory _factory;

    private final Config config;

    LdapSocketFactory(Config config) {
        this.config = config;
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("authType is %1$s.", authType));
                    logger.fine("cert issuers");
                    for (int i = 0; i < certs.length; i++) {
                        logger.fine(String.format("\t%1$s", certs[i]
                                .getIssuerX500Principal().getName()));
                        logger.fine(String.format("\t%1$s", certs[i]
                                .getIssuerDN().getName()));
                    }
                }
            }
        } };

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts,
                    new java.security.SecureRandom());
            _factory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object getCipherSuites() {
        return null;
    }

    public boolean isClientAuth() {
        return false;
    }

    public Socket makeSocket(String host, int port) throws LDAPException {
        try {
            Config socksConfig = config.getConfig("com.learningobjects.cpxp.socks");

            String socksHost = socksConfig.getString("proxyHost");
            int socksPort = socksConfig.getInt("proxyPort");
            Proxy proxy;
            if (!socksHost.isEmpty() && (socksPort > 0)) {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socksHost, socksPort));
            } else {
                proxy = Proxy.NO_PROXY;
            }
            Socket socket = new Socket(proxy);
            InetSocketAddress address = new InetSocketAddress(host, port);
            socket.connect(address);
            SSLSocket ssl = (SSLSocket) _factory.createSocket(socket, host, port, true);
            ssl.startHandshake();
            return ssl;
        } catch (UnknownHostException e) {
            throw new LDAPException(String.format("Uknown host: %1$s", host),
                    LDAPException.CONNECT_ERROR);
        } catch (IOException e) {
            throw new LDAPException(String.format(
                    "Error opening socket: %1$s:%2$d [%3$s]", host, port, e
                            .getMessage()), LDAPException.CONNECT_ERROR);
        }
    }
}
