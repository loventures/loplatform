// README: WARNING:
// Avoid new ECMAScript features, assume you are running in IE 5 or 6.

var frameName = 'lo-scorm';
var launchUrl = $$launchUrl;
var packageId = $$packageId;

function appendInput(form, name, value) {
    var input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
}

// TODO: record time spent?

function initLo(scorm) {
    var finished = false;

    if (!scorm.LMSInitialize('')) {
        alert('SCORM initialisation error')
        return;
    }

    function onMessage(event) {
        if (!launchUrl.startsWith(event.origin)) {
            console.log('SCORM origin mismatch', event.origin);
            return;
        }

        switch (event.data._type) {
            case 'INCOMPLETE':
                console.log('SCORM incomplete');
                scorm.LMSSetValue('cmi.core.lesson_status', 'incomplete');
                scorm.LMSCommit();
                break;

            case 'COMPLETE':
                const grade = event.data.grade;
                const max = event.data.max;
                const percent = 100.0 * grade / max; // Moodle drops the grade if max is not 100
                console.log('SCORM grade', grade, max, percent);
                scorm.LMSSetValue('cmi.core.score.min', 0.0);
                scorm.LMSSetValue('cmi.core.score.max', 100.0);
                scorm.LMSSetValue('cmi.core.score.raw', percent);
                scorm.LMSSetValue('cmi.core.lesson_status', 'completed');
                scorm.LMSCommit();
                break;

            case 'SESSION_EXPIRED':
                if (!finished) {
                    console.log('SCORM session expired');
                    scorm.LMSSetValue('cmi.core.exit', 'time-out');
                    scorm.LMSFinish('');
                    finished = true;
                }
                break;

            case 'LOGGED_OUT':
                if (!finished) {
                    console.log('SCORM logged out');
                    scorm.LMSSetValue('cmi.core.exit', 'logout');
                    scorm.LMSFinish('');
                    finished = true;
                }
                break;

            default:
                console.log('SCORM message unknown', event.data);
        }
    }

    window.addEventListener('message', onMessage, false);

    function onBeforeUnload() {
        if (!finished) {
            console.log('SCORM suspended');
            scorm.LMSSetValue('cmi.core.exit', 'suspend');
            scorm.LMSFinish('');
        }
    }

    window.addEventListener('beforeunload', onBeforeUnload);

    var studentId = scorm.LMSGetValue('cmi.core.student_id');
    var studentName = scorm.LMSGetValue('cmi.core.student_name');

    var body = document.body;

    var iframe = document.createElement('iframe');
    iframe.id = frameName;
    iframe.name = frameName;
    iframe.style = 'padding: 0; margin: 0; border: 0; width: 100vw; height: 100vh;';
    iframe.frameBorder = '0'; // IE
    body.appendChild(iframe);

    var form = document.createElement('form');
    appendInput(form, 'packageId', packageId);
    if (window.lo_pageId) appendInput(form, 'pageId', window.lo_pageId);
    appendInput(form, 'studentId', studentId);
    appendInput(form, 'studentName', studentName);
    appendInput(form, 'checksum', rstr2b64(rstr_hmac_sha1(packageId, studentId + '|' + studentName)));
    form.style = 'border: 0; padding: 0; margin: 0; width: 0; height: 0;';
    form.method = 'POST';
    form.action = launchUrl;
    form.target = frameName;
    body.appendChild(form);

    form.submit();
}

function onLoad() {
    function scanParentsForApi(win) {
        var nParentsSearched = 0;
        while ((win.API == null) && (win.parent != null) && (win.parent !== win) && (nParentsSearched <= 500)) {
            nParentsSearched++;
            win = win.parent;
        }
        return win.API;
    }

    function findScormApi() {
        var api = null;
        if ((window.parent != null) && (window.parent !== window)) {
            api = scanParentsForApi(window.parent);
        }
        if ((api == null) && (window.top.opener != null)) {
            api = scanParentsForApi(window.top.opener);
        }
        return api;
    }

    var attempts = 0;

    function loadApi() {
        var api = findScormApi();
        if (api) {
            initLo(api);
        } else if (attempts < 10) {
            ++ attempts;
            setTimeout(loadApi, 300);
        } else {
            alert('Error obtaining SCORM API.');
        }
    }
    loadApi(); // unclear if this looping attempt is necessary, but...
}

window.addEventListener('load', onLoad);

/*!
 * A JavaScript implementation of the Secure Hash Algorithm, SHA-1, as defined
 * in FIPS 180-1
 * Version 2.2 Copyright Paul Johnston 2000 - 2009.
 * Other contributors: Greg Holt, Andrew Kepert, Ydnar, Lostinet
 * Distributed under the BSD License
 * See http://pajhome.org.uk/crypt/md5 for details.
 *
 */

var b64pad  = "="; /* base-64 pad character. "=" for strict RFC compliance   */

function b64_sha1(s)    { return rstr2b64(rstr_sha1(str2rstr_utf8(s))); }

function rstr_sha1(s)
{
    return binb2rstr(binb_sha1(rstr2binb(s), s.length * 8));
}

function rstr_hmac_sha1(key, data) {
    var bkey = rstr2binb(key);
    if(bkey.length > 16) bkey = binb_sha1(bkey, key.length * 8);

    var ipad = Array(16), opad = Array(16);
    for(var i = 0; i < 16; i++)
    {
        ipad[i] = bkey[i] ^ 0x36363636;
        opad[i] = bkey[i] ^ 0x5C5C5C5C;
    }

    var hash = binb_sha1(ipad.concat(rstr2binb(data)), 512 + data.length * 8);
    return binb2rstr(binb_sha1(opad.concat(hash), 512 + 160));
}

function rstr2b64(input) {
    try { b64pad } catch(e) { b64pad=''; }
    var tab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    var output = "";
    var len = input.length;
    for(var i = 0; i < len; i += 3)
    {
        var triplet = (input.charCodeAt(i) << 16)
            | (i + 1 < len ? input.charCodeAt(i+1) << 8 : 0)
            | (i + 2 < len ? input.charCodeAt(i+2)      : 0);
        for(var j = 0; j < 4; j++)
        {
            if(i * 8 + j * 6 > input.length * 8) output += b64pad;
            else output += tab.charAt((triplet >>> 6*(3-j)) & 0x3F);
        }
    }
    return output;
}

function str2rstr_utf8(input) {
    var output = "";
    var i = -1;
    var x, y;

    while(++i < input.length)
    {
        /* Decode utf-16 surrogate pairs */
        x = input.charCodeAt(i);
        y = i + 1 < input.length ? input.charCodeAt(i + 1) : 0;
        if(0xD800 <= x && x <= 0xDBFF && 0xDC00 <= y && y <= 0xDFFF)
        {
            x = 0x10000 + ((x & 0x03FF) << 10) + (y & 0x03FF);
            i++;
        }

        /* Encode output as utf-8 */
        if(x <= 0x7F)
            output += String.fromCharCode(x);
        else if(x <= 0x7FF)
            output += String.fromCharCode(0xC0 | ((x >>> 6 ) & 0x1F),
                0x80 | ( x         & 0x3F));
        else if(x <= 0xFFFF)
            output += String.fromCharCode(0xE0 | ((x >>> 12) & 0x0F),
                0x80 | ((x >>> 6 ) & 0x3F),
                0x80 | ( x         & 0x3F));
        else if(x <= 0x1FFFFF)
            output += String.fromCharCode(0xF0 | ((x >>> 18) & 0x07),
                0x80 | ((x >>> 12) & 0x3F),
                0x80 | ((x >>> 6 ) & 0x3F),
                0x80 | ( x         & 0x3F));
    }
    return output;
}

function rstr2binb(input) {
    var output = Array(input.length >> 2);
    for(var i = 0; i < output.length; i++)
        output[i] = 0;
    for(var i = 0; i < input.length * 8; i += 8)
        output[i>>5] |= (input.charCodeAt(i / 8) & 0xFF) << (24 - i % 32);
    return output;
}

function binb2rstr(input) {
    var output = "";
    for(var i = 0; i < input.length * 32; i += 8)
        output += String.fromCharCode((input[i>>5] >>> (24 - i % 32)) & 0xFF);
    return output;
}

function binb_sha1(x, len) {
    /* append padding */
    x[len >> 5] |= 0x80 << (24 - len % 32);
    x[((len + 64 >> 9) << 4) + 15] = len;

    var w = Array(80);
    var a =  1732584193;
    var b = -271733879;
    var c = -1732584194;
    var d =  271733878;
    var e = -1009589776;

    for(var i = 0; i < x.length; i += 16)
    {
        var olda = a;
        var oldb = b;
        var oldc = c;
        var oldd = d;
        var olde = e;

        for(var j = 0; j < 80; j++)
        {
            if(j < 16) w[j] = x[i + j];
            else w[j] = bit_rol(w[j-3] ^ w[j-8] ^ w[j-14] ^ w[j-16], 1);
            var t = safe_add(safe_add(bit_rol(a, 5), sha1_ft(j, b, c, d)),
                safe_add(safe_add(e, w[j]), sha1_kt(j)));
            e = d;
            d = c;
            c = bit_rol(b, 30);
            b = a;
            a = t;
        }

        a = safe_add(a, olda);
        b = safe_add(b, oldb);
        c = safe_add(c, oldc);
        d = safe_add(d, oldd);
        e = safe_add(e, olde);
    }
    return Array(a, b, c, d, e);
}

function sha1_ft(t, b, c, d) {
    if(t < 20) return (b & c) | ((~b) & d);
    if(t < 40) return b ^ c ^ d;
    if(t < 60) return (b & c) | (b & d) | (c & d);
    return b ^ c ^ d;
}

function sha1_kt(t) {
    return (t < 20) ?  1518500249 : (t < 40) ?  1859775393 :
        (t < 60) ? -1894007588 : -899497514;
}

function safe_add(x, y) {
    var lsw = (x & 0xFFFF) + (y & 0xFFFF);
    var msw = (x >> 16) + (y >> 16) + (lsw >> 16);
    return (msw << 16) | (lsw & 0xFFFF);
}

function bit_rol(num, cnt) {
    return (num << cnt) | (num >>> (32 - cnt));
}
