/* eslint-disable */

/*!
 * A JavaScript implementation of the Secure Hash Algorithm, SHA-1, as defined
 * in FIPS 180-1
 * Version 2.2 Copyright Paul Johnston 2000 - 2009.
 * Other contributors: Greg Holt, Andrew Kepert, Ydnar, Lostinet
 * Distributed under the BSD License
 * See http://pajhome.org.uk/crypt/md5 for details.
 *
 * Updated to fit this into the webpack framework
 */

var b64pad = '=';
export function b64_sha1(a) {
  return rstr2b64(rstr_sha1(str2rstr_utf8(a)));
}
export function rstr_sha1(a) {
  return binb2rstr(binb_sha1(rstr2binb(a), a.length * 8));
}
export function rstr_hmac_sha1(c, f) {
  var e = rstr2binb(c);
  if (e.length > 16) {
    e = binb_sha1(e, c.length * 8);
  }
  var a = Array(16),
    d = Array(16);
  for (var b = 0; b < 16; b++) {
    a[b] = e[b] ^ 909522486;
    d[b] = e[b] ^ 1549556828;
  }
  var g = binb_sha1(a.concat(rstr2binb(f)), 512 + f.length * 8);
  return binb2rstr(binb_sha1(d.concat(g), 512 + 160));
}
export function rstr2b64(c) {
  try {
    b64pad;
  } catch (h) {
    b64pad = '';
  }
  var g = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  var b = '';
  var a = c.length;
  for (var f = 0; f < a; f += 3) {
    var k =
      (c.charCodeAt(f) << 16) |
      (f + 1 < a ? c.charCodeAt(f + 1) << 8 : 0) |
      (f + 2 < a ? c.charCodeAt(f + 2) : 0);
    for (var d = 0; d < 4; d++) {
      if (f * 8 + d * 6 > c.length * 8) {
        b += b64pad;
      } else {
        b += g.charAt((k >>> (6 * (3 - d))) & 63);
      }
    }
  }
  return b;
}
export function str2rstr_utf8(c) {
  var b = '';
  var d = -1;
  var a, e;
  while (++d < c.length) {
    a = c.charCodeAt(d);
    e = d + 1 < c.length ? c.charCodeAt(d + 1) : 0;
    if (55296 <= a && a <= 56319 && 56320 <= e && e <= 57343) {
      a = 65536 + ((a & 1023) << 10) + (e & 1023);
      d++;
    }
    if (a <= 127) {
      b += String.fromCharCode(a);
    } else {
      if (a <= 2047) {
        b += String.fromCharCode(192 | ((a >>> 6) & 31), 128 | (a & 63));
      } else {
        if (a <= 65535) {
          b += String.fromCharCode(224 | ((a >>> 12) & 15), 128 | ((a >>> 6) & 63), 128 | (a & 63));
        } else {
          if (a <= 2097151) {
            b += String.fromCharCode(
              240 | ((a >>> 18) & 7),
              128 | ((a >>> 12) & 63),
              128 | ((a >>> 6) & 63),
              128 | (a & 63)
            );
          }
        }
      }
    }
  }
  return b;
}
export function rstr2binb(b) {
  var a = Array(b.length >> 2);
  for (var c = 0; c < a.length; c++) {
    a[c] = 0;
  }
  for (var c = 0; c < b.length * 8; c += 8) {
    a[c >> 5] |= (b.charCodeAt(c / 8) & 255) << (24 - (c % 32));
  }
  return a;
}
export function binb2rstr(b) {
  var a = '';
  for (var c = 0; c < b.length * 32; c += 8) {
    a += String.fromCharCode((b[c >> 5] >>> (24 - (c % 32))) & 255);
  }
  return a;
}
export function binb_sha1(v, o) {
  v[o >> 5] |= 128 << (24 - (o % 32));
  v[(((o + 64) >> 9) << 4) + 15] = o;
  var y = Array(80);
  var u = 1732584193;
  var s = -271733879;
  var r = -1732584194;
  var q = 271733878;
  var p = -1009589776;
  for (var l = 0; l < v.length; l += 16) {
    var n = u;
    var m = s;
    var k = r;
    var h = q;
    var f = p;
    for (var g = 0; g < 80; g++) {
      if (g < 16) {
        y[g] = v[l + g];
      } else {
        y[g] = bit_rol(y[g - 3] ^ y[g - 8] ^ y[g - 14] ^ y[g - 16], 1);
      }
      var z = safe_add(
        safe_add(bit_rol(u, 5), sha1_ft(g, s, r, q)),
        safe_add(safe_add(p, y[g]), sha1_kt(g))
      );
      p = q;
      q = r;
      r = bit_rol(s, 30);
      s = u;
      u = z;
    }
    u = safe_add(u, n);
    s = safe_add(s, m);
    r = safe_add(r, k);
    q = safe_add(q, h);
    p = safe_add(p, f);
  }
  return Array(u, s, r, q, p);
}
export function sha1_ft(e, a, g, f) {
  if (e < 20) {
    return (a & g) | (~a & f);
  }
  if (e < 40) {
    return a ^ g ^ f;
  }
  if (e < 60) {
    return (a & g) | (a & f) | (g & f);
  }
  return a ^ g ^ f;
}
export function sha1_kt(a) {
  return a < 20 ? 1518500249 : a < 40 ? 1859775393 : a < 60 ? -1894007588 : -899497514;
}
export function safe_add(a, d) {
  var c = (a & 65535) + (d & 65535);
  var b = (a >> 16) + (d >> 16) + (c >> 16);
  return (b << 16) | (c & 65535);
}
export function bit_rol(a, b) {
  return (a << b) | (a >>> (32 - b));
}
