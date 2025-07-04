// TODO: param for subactions? nop

function detectBrowser() {
  // if we are in some old browser
  if (!window.XMLHttpRequest) {
    document.write("${msg.underground_unsupportedBrowser}");
  }
}

function checkZip(response) {
    if (/^application\/zip/.test(response.mimeType)) {
        return true;
    } else {
        ug.error(null, { title: "${msg.underground_zip_error_title}",
                         message: "${msg.underground_zip_error_message} (" + response.mimeType + ")" });
        return false;
    }
}

function cancelAction(history, url) {
    window.history.back();
    return false;
}

/**
* Would be so much easier in ES6! function* asyncGen() { yield 1; while(true) yield 5; }
*/
function asyncIntervalGenerator() {
    var state = 0;
    return {
        next: function () {
            switch (state) {
                case 0:
                    state = 1;
                    return 1;
                case 1:
                    return 5
            }
        }
    }
}

Ext = {
};

/* TODO: Move these into the ug namespace. */
/* Indicates whether the browser supports the HTML 5 postMessage method. */
hasPostMessage = window["postMessage"] && !Ext.isOpera;
/* Counts calls to defeat caching. */
postMessageCounter = 0;

ug = {
    dirtyElements: { },
    scripts: {},
    zIndex: 12000,

    openMenu: null,

    init: function(cf) {
        Ext.apply(ug, cf, { initialized: true, dirty: false });
        Ext.BLANK_IMAGE_URL = ug.staticUrl + '/images/blank.gif';
        ug.host = document.location.href.replace(new RegExp('^([^/]*//[^/]*/).*$'), '$1');
        ug.onReady(function() {
                       /* For UG-3905. The return value is used for the confirmation dialog. */
                       window.onbeforeunload = ug.beforeUnload;
                       Ext.EventManager.onWindowResize(ug.layout);
                       ug.layout();
                       ug.initHyperlinks();
                       ug.initSearch();
                       if(ug.autofocus) {
                           Ext.fly(ug.autofocus).focus();
                       }
                   });
    },

    onReady: function(func) {
       Ext.onReady(function() {
           try {
               func();
           } catch (ex) {
               ug.jsreport(ex);
           }
       });
    },

    autoFocus: function(el) {
        ug.onReady(function() {
            var element = Ext.get(el);
            if (element) {
              element.focus();
            }
        });
    },

    doLogin: function() {
        if (ug.onLogin()) {
            document.forms.loginForm.submit();
        }
    },

    onLogin: function() {
        var form = document.forms.loginForm, id = form.mechanism.value, mechanism = null;
        Ext.each(ug.loginMechanisms, function(m) {
            if (id == m.id) {
                mechanism = m;
            }
        });

        if (mechanism && (mechanism.type == 'sso')) {
            var loc = encodeURIComponent(document.location.href);
            var url = mechanism.url.replace('%%URL%%', encodeURIComponent(loc));
            document.location.href = url.replace('%URL%', loc);
            return false;
        }

        if (form.username.value == '') {
            Ext.fly(form.username).focus();
            return false;
        } else if (form.password.value == '') {
            Ext.fly(form.password).focus();
            return false;
        }

        // only use CHAP when the static fields are in use and when there is no SSL
        if (!form.action.match(/^https/)) {
            ug.password = form.password.value;
            var key = rstr_sha1(str2rstr_utf8(ug.domainId + '/' + form.username.value + String.fromCharCode(0) + ug.password));
            form.response.value = rstr2b64(rstr_hmac_sha1(key, str2rstr_utf8(form.challenge.value + '/' + form.timestamp.value)));
            form.credential.value = '';
            form.password.value = '';
        }

        ug.loginWindow.setTitle("${msg.underground_loggingIn_title}");
        ug.loginWindow.disable();

        return true;
    },

    unLogin: function() {
        ug.loginWindow.hide();
    },

    parentWindowCall: function(fn) {
        try {
            if (!ug.noParent && (window.parent != window) && window.parent.ug) {
                window.parent.ug[fn].apply(window.parent.ug, [].slice.apply(arguments).slice(1));
                return true;
            }
        } catch (ignored) {
            ug.noParent = true;
        }
        return false;
    },

    setDirty: function(id) {
        if (ug.parentWindowCall('setDirty', id)) {
            return;
        }
        if (!ug.dirty) {
            ug.oldTitle = document.title;
            document.title = ug.oldTitle + ' *';
            ug.update('pageTitle-modified', '*');
        }
        if (id != undefined) {
            ug.update('dirty-' + id, '*');
            ug.dirtyElements['dirty-' + id] = true;
        }
        ug.dirty = true;
    },

    isDirty: function(id) {
        return ug.dirtyElements['dirty-' + id];
    },

    setClean: function(id) {
        if (ug.parentWindowCall('setClean', id)) {
            return;
        }
        if (ug.dirty) { // currently dirty
            var fullyClean = true;

            if (id != undefined) {
                delete ug.dirtyElements['dirty-' + id];

                // check for remaining dirty elements
                for (element in ug.dirtyElements) {
                    if (ug.dirtyElements[element] == true) {
                        fullyClean = false;
                        break;
                    }
                }
                var dirty = Ext.fly('dirty-' + id);
                if (dirty) {
                    dirty.update('');
                }
            }

            if (fullyClean) { // there are no dirty elements remaining, or this is a full page clean
                ug.dirty = false;
                ug.dirtyElements = {};
                document.title = ug.oldTitle;
                ug.update('pageTitle-modified', '');
            }
        }
    },

    log: function() {
        if (window.console && window.console.log && (typeof(window.console.log.apply) == 'function')) {
            window.console.log.apply(window.console, arguments);
        }
    },

    isBlank: function(s) {
        return Ext.isEmpty(s, true);
    },

    blankIfEmpty: function(s) {
        return Ext.isEmpty(s) ? '' : s;
    },

    sortable: function() {
        var s = '';
        for (var i = 0; i < arguments.length; ++ i) {
            if (i > 0) {
                s = s + '\uffff';
            }
            s = s + ug.blankIfEmpty(arguments[i]);
        }
        return s;
    },

    userSorter: function(u) {
        return ug.sortable(u.familyName, u.givenName, u.middleName, u.id);
    },

    userRenderer: function(u) {
        return Ext.util.Format.htmlEncode(Ext.isEmpty(u.middleName, true)
                                          ? (u.givenName + ' ' + u.familyName)
                                          : (u.givenName + ' ' + u.middleName + ' ' + u.familyName));
    },

    getViewport: function() {
        var doc = document,
        docElement = doc.documentElement,
        docBody = doc.body,
        scrollX = (docElement.scrollLeft || docBody.scrollLeft || 0),
        scrollY = (docElement.scrollTop || docBody.scrollTop || 0);
        return { left: scrollX,
                 top: scrollY,
                 width: Ext.lib.Dom.getViewWidth(),
                 height: Ext.lib.Dom.getViewHeight() };
    },

    beforeUnload: function(e) {
        /* This is a bit horrendous but there is no ready solution:
         * If I dirty the page, navigate away and then hit Cancel
         * to stay on the page then leaving will remain set. Not
         * clear what event could be used to unset leaving... */
        ug.leaving = true;
        if (ug.dirty) {
            /* Fixes UG-3905. WebKit and Opera do not support browserEvent.returnValue. The value returned here is used
             * for the confirmation dialog. Note: this handler must be assigned directly to window.onbeforeunload for
             * correct behavior. See http://webreference.com/dhtml/diner/beforeunload/bunload4.html for details. */
            return "${msg.underground_dirty_warning_message}";
        }
    },

    popdown: function() {
        if (ug._glass && !ug._glass.isDestroyed) {
            ug._glass.destroy();
        }
        delete ug._glass;
    },

    glass: function(on) {
        if (!on) {
            if (ug._glass && !ug._glass.isDestroyed) {
                ug._glass.destroy();
            }
        } else if (!ug._glass || ug._glass.isDestroyed) {
            var glass = new Ext.Component({
                autoRender: true,
                cls: 'cp_frosted'
            });
            ug._glass = glass;
            glass.show();
            ug.blur();
            var syncSize = function() {
                if (ug._glass) {
                    var D = document,
                    h = Math.max(Math.max(D.body.scrollHeight, D.documentElement.scrollHeight),
                                 Math.max(D.body.offsetHeight, D.documentElement.offsetHeight),
                                 Math.max(D.body.clientHeight, D.documentElement.clientHeight));
                    ug._glass.el.setSize({ width: Ext.getBody().dom.scrollWidth,
                                           height: h });
                }
            };
            syncSize();
            Ext.EventManager.onWindowResize(syncSize); // TODO: this screws up on shrinking
        }
        return ug._glass;
    },

    popup: function(data) {
        var glass = ug.glass(true);
        glass.el.on('click', glass.destroy, glass);
        var km = Ext.getDoc().addKeyListener(27, glass.destroy, glass);
        glass.on('destroy', km.disable, km);
        if (data.ondestroy) {
            glass.on('destroy', data.ondestroy, data.scope || data);
        }
        var cb = function(response) {
            if (glass.isDestroyed) {
                return;
            }
            var popup = new Ext.Component({
                autoRender: true,
                cls: 'portlet cp_popup'
            });
            popup.show();
            popup.update(response, true);
            var size = popup.el.getSize(), view = ug.getViewport();
            // ug.log(size, view);
            var x = Ext.isNumber(data.x) ? data.x : Math.round(view.left + view.width / 2);
            var y = Ext.isNumber(data.y) ? data.y : Math.round(view.top + view.height / 2);
            if (!data.anchor || (data.anchor == 'c')) {
                x -= Math.round(size.width / 2);
                y -= Math.round(size.height / 2);
            } else if (data.anchor == 'e') {
                y -= Math.round(size.height / 2);
            } else if (data.anchor == 'w') {
                x -= size.width;
                y -= Math.round(size.height / 2);
            } else if (data.anchor == 's') {
                x -= Math.round(size.width / 2);
            }
            var vs = Ext.getBody().getScroll(), vw = Ext.lib.Dom.getViewWidth(), vh = Ext.lib.Dom.getViewHeight();
            x = Math.max(vs.left + 4, Math.min(vs.left + vw - size.width - 4, x));
            y = Math.min(vs.top + vh - size.height - 4, Math.max(vs.top + 4, y));
            popup.el.moveTo(x, y);
            // popup.el.child('a').focus();
            glass.on('destroy', popup.destroy, popup);
            if (data.onpopup) {
                data.onpopup.call(data.scope || window, popup);
            }
        };
        if (data.url) {
            ug.rpc({ url: data.url, params: data.params, callback: cb, errafter: glass.destroy.createDelegate(glass) });
        } else {
            cb(data.html);
        }
    },

    popupActions: function(a, id) {
        return ug.showActions(a, '/sys/popup/userActions', { user : id });
    },

    permalinkActions: function(a, id) {
        return ug.showActions(a, '/sys/popup/permalinkActions', { item : id });
    },

    showActions: function(a, rpc, params) {
        var el = Ext.get(a), region = el.getRegion();
        var anchor, x, y = Math.round((region.top + region.bottom) / 2);
        // is the center of the hyperlink on the left of the screen (divide lhs and rhs by 2 for clarity)
        if (region.left + region.right < Ext.getBody().getWidth()) {
            anchor = 'e';
            x = region.right;
        } else {
            anchor = 'w';
            x = region.left;
        }
        el.addClass('cp_active');
        ug.popup({ url: rpc,
                   params:  params,
                   anchor: anchor,
                   x: x,
                   y: y,
                   ondestroy: el.removeClass.createDelegate(el, [ 'cp_active' ]) });
        return false;
    },

    help: function(a) {
        var el = Ext.fly('helpMenu');
        if (el.isDisplayed()) {
            Ext.getBody().un('click', ug.help);
            el.setDisplayed(false);
        } else {
            el.setDisplayed(true);
            Ext.getBody().on.defer(1, Ext.getBody(), [ 'click', ug.help ]);
        }
    },

    menu: function(a) {
        if (!a.browserEvent) {
            ug.openMenu = Ext.fly(a).next();
        }
        if (ug.openMenu.isDisplayed()) {
            Ext.getBody().un('click', ug.menu);
            ug.openMenu.setDisplayed(false);
            delete ug.openMenu;
        } else {
            ug.openMenu.setDisplayed(true);
            Ext.getBody().on.defer(1, Ext.getBody(), [ 'click', ug.menu ]);
        }
    },

    print: function() {
        window.print();
    },

    asCmp: function(id){
        var el = Ext.get(id);
        return {
            getEl: function() { return el; },
            focus: function() { el.focus(); }
        }
    },

    keptAlive: new Date().getTime(),

    keepAlive: function(cb) {
        var now = new Date().getTime();
        if (now - ug.keptAlive >= 15 * 60 * 1000) {
            ug.keptAlive = now;
            ug.rpc('/dev/null', {}, cb); // meh, better url
        } else {
            cb();
        }
    },

    getHttpUrl: function(path) {
        var port = window.location.port;
        if (window.location.protocol == 'https:') {
            port = (port && (port != 443)) ? Number(port) - 101 : '';
        } else {
            port = (port && (port != 80)) ? port : '';
        }
        return 'http://' + window.location.hostname + (port ? ':' + port : '') + (path || '/');
    },

    getHttpsUrl: function(path) {
        var port = window.location.port;
        if (window.location.protocol == 'https:') {
            port = (port && (port != 443)) ? port : '';
        } else {
            port = (port && (port != 80)) ? Number(port) + 101 : '';
        }
        return 'https://' + window.location.hostname + (port ? ':' + port : '') + (path || '/');
    },

    reload: function() {
        window.location.href = window.location.href.replace(/#.*/, '');
    },

    reloadStrict: function() {
        window.location.reload();
    },

    goBack: function() {
        window.location.href = window.location.href.replace(/#.*/, '').replace(/\?.*/, '').replace(/!.*/, '');
    },

    redirect: function(href) {
        ug.go((typeof(href) == 'object') ? href.url : href);
    },

    go: function(href, blank) {
        if (href && (href != '')) {
            if (blank === '_top') {
                window.top.location.href = href;
            } else if (blank) {
                window.open(href, '_blank');
            } else if (href[0] == '#') {
                window.location.hash = href;
            } else {
                window.location.href = href;
            }
        }
    },

    goParent: function(href) {
        if (href && (href != '')) {
            window.open(href, '_top');
        }
    },

    goWhenAvailable: function(a, url) {
        var link = Ext.get(a);
        if (!link.hasClass("unavailableLink")) {
            ug.go(url);
        }
    },

    ideaExchange: function(a) {
        ug.go('/control/dynamic/IdeaExchange.go', true);
    },

    escapeMeekView: function() {
        ug.goParent('/control/dynamic/IntegrationRpc.escapeMeekView?redirect=' + encodeURIComponent(document.location.href));
    },

    // stopPropagation: Ext.lib.Event.stopPropagation,

    clearDesktopViewCookie: function(a) {
        ug.setCookie({ name: 'desktopView', expires: -1 });
        ug.reload();
    },

    viewCookies: function(a) {
        var cookies = document.cookie.split(";");

        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i];
            ug.log(cookie);
        }
        //ug.reload();
    },

    reveal: function(revealer) {
        var revealerElement = Ext.get(revealer);
        revealerElement = revealerElement.parent('.fromReveal') || revealerElement;
        // if you think this is bad, you're not going to like ARIA
        var parent = revealerElement.parent(".revealer");
        var element = parent.child(".toReveal");
        element.setVisibilityMode(Ext.Element.DISPLAY);
        if (revealerElement.hasClass("open")) {
            revealerElement.replaceClass('open', 'closed');
            element.hide();
        } else {
            revealerElement.replaceClass('closed', 'open');
            element.removeClass('unrevealed');
            element.show(true);
        }
    },

    /* Upload */

    showUploadForm: function(a, id, title) {
        var win = ug[id + '-uploadWindow'];
        if (!win) {
            var doUpload = function() {
                         // really want to disable the whole window but
                         // that would render status illegible
                         win.disableButtons();
                         document.forms[id + '-uploadForm'].onsubmit();
            };
            win = ug[id + '-uploadWindow'] = new ug.Dialog({
                 id: id + '-uploadDialog',
                 title: title,
                 width: 515,
                 contentEl: id + '-uploadDiv',
                 defaultButton: ug.asCmp(id + '-upload'),
                 /*okay: {
                     text: "${msg.underground_upload_action_upload}",
                     handler: doUpload
                 },*/
                 cancel: "${msg.underground_upload_action_cancel}",
                 listeners: {
                     beforeshow: function() {
                         document.forms[id + '-uploadForm'].reset();
                         ug.update(id + '-uploadStatus', '&nbsp;');
                         if (!this.timer) {
                             this.timer = new Ext.util.TaskRunner();
                         }
                         this.timer.start({ run: function() {
                                                if (document.forms[id + '-uploadForm'].upload.value != '') {
                                                    this.timer.stopAll();
                                                    doUpload();
                                                }
                                            }, interval: 100, scope: this });
                     },
                     beforehide: function() {
                         this.timer.stopAll();
                         if (this.uploader) {
                             this.uploader.cancelUpload();
                         }
                         window.frames.uploadFrame.location.replace(ug.staticUrl + '/blank.html');
                     }
                 }
               });
        }
        win.showEl = a;
        win.show(a);
    },

    error: function(a, cf) {
        var guidStr = cf.guid ? ' (ID: ' + cf.guid + ')' : '';
        ug.alert(a, { title: cf.title || "Error", width: 400,
                       message: (cf.message || "An error occurred.") + guidStr,
                       listeners: cf.listeners });
    },

    rpcForm: function(form, o) {
        o.form = form;
        o.params = {};
        var inputs = Ext.query('input,select,textarea', form);
        Ext.each(inputs, function(input) {
            if (input.name != '' && input.id != 'navBack' && input.id != 'navHistory') {
                if (input.type == 'radio' && !input.checked) {
                    return;
                }
                if (input.type == 'checkbox') {
                    o.params[input.name] = input.checked ? "on" : "off";
                } else {
                    o.params[input.name] = input.value;
                }
            }
        });

        ug.rpc(o);
    },

    scrape: function(form, encode) {
        var values = {};
        Ext.each(Ext.getDom(form).elements, function(input) {
            var type = input.type, value = null;
            if (input.disabled || (input.name == '') || input.localName == 'fieldset') {
            } else if ((type == 'checkbox') || (type == 'radio')) {
                if (input.checked) {
                    value = Ext.util.Format.trim(input.value);
                }
            } else if (type == 'select-one') {
                if (input.selectedIndex >= 0) {
                    var opt = input.options[input.selectedIndex];
                    if (!opt.disabled) {
                        value = opt.value;
                    }
                }
            } else if (type == 'select-multiple') {
                value = [];
                Ext.each(input.options, function(opt) {
                    if (opt.selected) {
                        value.push(opt.value);
                    }
                });
            } else {
                value = Ext.util.Format.trim(input.value);
            }
            if (value) {
                var name = input.name, index = name.indexOf(':'), map = values;
                while (encode && (index >= 0)) {
                    var subname = name.substring(0, index), submap = map[subname];
                    name = name.substring(1 + index);
                    index = name.indexOf(':');
                    if (!submap) {
                        map[subname] = submap = name.match(/^\d/) ? [] : {};
                    }
                    map = submap;
                }
                var old = map[name];
                if (typeof(old) == 'string') {
                    value = [ old, value ];
                } else if (typeof(old) == 'object') {
                    old.push(value);
                    value = old;
                }
                map[name] = value;
            }
        });
        return (!encode || (encode == 'object')) ? values : Ext.encode(values); // TODO: fix horrible api hack
    },

    rpcupdate: function(rpc, params, id, cb, scope) {
        if (typeof(params) == 'string') {
            cb = id;
            id = params;
            params = {};
        }
        ug.rpc(rpc, params, function(response) { ug.update(id, response, false, true); if (cb) cb.call(scope || window); });
    },

    rpcappend: function(rpc, params, id, cb, scope) {
        if (typeof(params) == 'string') {
            cb = id;
            id = params;
            params = {};
        }
        ug.rpc(rpc, params, function(response) { ug.append(id, response, false, true); if (cb) cb.call(scope || window); });
    },

    blur: function() {
        if (document.activeElement) {
            Ext.fly(document.activeElement).blur();
        }
    },

    rpc: function(data, params, callback, scope) {
        if (typeof(data) == 'string') {
            var isUrl = (data.charAt(0) == '/') || (data.indexOf(':') >= 0);
            var isForm = Ext.isElement(params);
            data = { rpc: isUrl ? null : data, url: isUrl ? data : null, params: isForm ? null : params, form: isForm ? params : null, callback: callback, scope: scope };
        }
        if (ug.disableRpc[data.rpc || data.url]) {
            // e.g. hit enter multiply in the comment subject
            if (data.disabledCallback) {
                data.disabledCallback.call(data.scope || this);
            }

            return;
        }
        var params = { formCookie: ug.formCookie };
        if (Ext.isString(data.params)) {
            Ext.apply(params, ug.scrape(data.params));
        } else if (data.params) {
            Ext.apply(params, data.params);
        } else if (data.form) {
            data.params = Ext.apply(params, ug.scrape(data.form));
        }
        var fel = Ext.get(data.form || data.button); // hack in the button case..
        if (fel) {
            var container = data.form ? fel : fel.parent();
            fel.addClass('submitting');
            container.select('.validation-error').setStyle('display', '');
        }
        var undisable;
        if (data.dialog) {
            if (data.disableDialog !== false) {
                data.dialog.disable();
            }
            undisable = function() { if (data.hideDialog !== false) { data.dialog.hide(); } };
        } else {
            undisable = ug.disable(data.disable, data.rpc || data.url, fel, data.glass);
        }
        var retryCb = function() {
            if (!data.dialog) {
                undisable(); // TODO: FIXME: don't do this...
            }
            ug.rpc(data);
        };
        var handleError = function(result) {
            if (result && fel && (result.status == 'invalid')) {
                var displayValidationMessage = function(field, message) {
                    var eel = null, vel = null;
                    if (field) {
                        eel = Ext.get(fel.dom.elements[field]) || Ext.get(fel.dom.elements[fel.id + ':' + field]); // TODO: multivalue fields
                        if (eel) {
                            var eel2 = (eel.hasClass('placeholder') || eel.parent().hasClass('x-form-field-wrap')) ? eel.parent() : eel;
                            vel = fel.child('.validate-' + field.replace(':', '_')) || eel2.next('.validate-field');
                        }
                    }
                    if (!vel) {
                        vel = fel.child('.validate-form') || fel.next('.validate-form');
                    }
                    if (data.dialog) {
                        data.dialog.enable();
                    } else {
                        undisable();
                    }
                    vel.update(message);
                    ug.displayEl(vel.dom);
                    if (eel) {
                        eel.focus();
                    }
                };

                if (result.errors) {
                    // legacy validation
                    Ext.iterate(result.errors, function(field, message) {
                        displayValidationMessage(field, message);
                    });
                } else {
                    displayValidationMessage(result.field, result.message);
                }
                if (data.errback) {
                    data.errback.call(data.scope || this, result);
                }
                return true;
            } else if (!result || ((result.status != 'error') && (result.status != 'invalid')) || /*legacy*/((result.status == 'invalid') && result.errors)) {
                return false;
            }
            if (result.logout) {
                var el = Ext.getDom('pageLayout-userLink');
                if (el) {
                    Ext.fly(el).addClass('loginExpired');
                }
            }
            if (data.errback) {
                data.errback.call(data.scope || this, result);
            }
            undisable();
            if (data.dialog && (data.hideDialog == false)) {
                data.dialog.enable();
            }
            ug.error(null, { title: result.title,
                              message: result.message,
                              guid: result.guid,
                              listeners: data.errafter ? { hide: data.errafter, scope: data.scope } : null });
            return true;
        };
        var failureCb = function(responseObject) {
            var contentType = !responseObject.getResponseHeader ? 'unknown' : responseObject.getResponseHeader('Content-Type');
            if (/application\/json/.test(contentType) && handleError(Ext.decode(responseObject.responseText))) {
                return;
            }
            if (data.failback && (data.failback.call(data.scope) === false)) {
                return;
            }
            if (!ug.leaving) {
                var dialog = new ug.Dialog2({ title: "${msg.underground_network_error_title}",
                                              body: "${msg.underground_network_error_message}",
                                              defaultButton: 'okay',
                                              okay: (data.retry == false) ? null : { label: "${msg.underground_network_error_action_retry}", handler: retryCb, autoHide: true },
                                              cancel: { label: "${msg.underground_network_error_action_cancel}", handler: undisable, autoHide: true },
                                              listeners: data.errafter ? { hide: data.errafter, scope: data.scope } : null
                                            });
                dialog.show();
            }
        };
        var successCb = function(responseObject) {
            if (responseObject.status == 202) {
                var result = Ext.decode(responseObject.responseText);
                if (result.status === 'async') {
                    var interval = ug.intervalGenerator(data.url).next();
                    if (data.progress && (typeof(result.percent) === 'number') && (result.percent >= 0)) {
                        Ext.fly(data.progress).show();
                        var transition = $.browser.webkit ? '-webkit-transition' : // TODO: Use jquery transit
                        $.browser.mozilla ? '-moz-transition' :  $.browser.msie ? '-ms-transition' :
                            $.browser.opera  ? '-o-transition' : 'transition';
                        var css = { width : result.percent + '%' };
                        css[transition] = 'width ' + interval + 's';
                        ug.flyChild(data.progress, '.bar').setStyle(css);
                    }
                    data.params.poll = true;
                    setTimeout(retryCb, interval * 1000);
                } else if (result.status == 'challenge') {
                    data.params['ug:response'] = rstr2b64(rstr_sha1(str2rstr_utf8(result.title)));
                    retryCb();
                } else if (result.status == 'confirm') {
                    var confirmCb = function(dialog) {
                        data.params['ug:confirmed'] = true;
                        data.dialog = dialog;
                        undisable();
                        retryCb();
                    };
                    ug.confirm(null, { title: result.title, message: result.message, cancel: undisable, okay: { handler: confirmCb, autoHide: false } });
                }
                return;
            }
            var contentType = responseObject.getResponseHeader('Content-Type');
            var result;
            if (/application\/json/.test(contentType)) {
                result = Ext.decode(responseObject.responseText);
                if (handleError(result)) {
                    return;
                }
            } else if (/text\/xml/.test(contentType)) {
                result = responseObject.responseXML;
            } else if (/text\/html/.test(contentType)) {
                result = responseObject.responseText;
            }
            if (!data.noEnable && (data.callback != ug.reload) && (data.callback != ug.redirect) && (data.callback != ug.goBack)) {
                undisable();
            } else {
                delete ug.disableRpc[data.rpc || data.url];
            }
            data.callback.call(data.scope || this, result, contentType, responseObject);
        };
        var conn = new ug.Connection();
        conn.request(
            {
                url: data.url || ("/control/dynamic/" + data.rpc),
                method: data.method || 'POST',
                params: params,
                timeout: data.timeout || 300000,
                success: successCb,
                failure: failureCb
            });
    },

    intervalStore: {},

    intervalGenerator: function(url) {
        var gen = ug.intervalStore[url];
        if (!gen || !(typeof gen.next === 'function')) {
            gen = asyncIntervalGenerator();
            ug.intervalStore[url] = gen;
        }
        return gen;
    },

    foldLeft: function(a, f, o) {
        for (var i = a.length - 1; i >= 0; -- i) {
            o = f(a[i], o);
        }
        return o;
    },

    disableRpc: {},

    disable: function(els, rpc, fel, glass) {
        var disabled = [];
        // This i done like this for scoping reasons; else all the undisable
        // functions see the last value of el from inside the loop. Not sure
        // what the correct solution to this is.
        var undisableA = function(a, onclick) {
            disabled.push(function() {
                              a.onclick = onclick;
                          });
        };
        var undisableInput = function(input) {
            disabled.push(function() {
                              input.disabled = false;
                          });
        };
        var undisableElement = function(element) {
            disabled.push(function() {
                              element.enable();
                          });
        }
        if (glass) {
            disabled.push(function() {
                             ug.glass(false);
                          });
            ug.glass(true);
        }
        if (fel || els) {
            if (rpc) {
                ug.disableRpc[rpc] = true;
            }
        }
        if (fel) {
            disabled.push(function() {
                fel.removeClass('submitting');
            });
        }
        var disableThing = function(el) {
            if (el && el.nodeName) {
                if (el.nodeName == 'A') {
                    undisableA(el, el.onclick);
                    el.onclick = function() { return false; };
                } else if (((el.nodeName == 'INPUT') || (el.nodeName == 'SELECT') || (el.nodeName == 'BUTTON')) && !el.disabled) {
                    undisableInput(el);
                    el.disabled = true;
                }
            } else if (el && el.dom) {
                undisableElement(el);
                el.disable();
            }
        };
        if (els) {
            if (typeof(els) == 'string') {
                var list = els.split(',');
                for (var i = 0; i < list.length; ++ i) {
                    disableThing(Ext.getDom(list[i]));
                }
            } else {
                if (els.length) { // is a list
                    for (var i = 0; i < els.length; ++ i) {
                        disableThing(els[i]);
                    }
                } else { // is a single element
                    disableThing(els);
                }
            }
        }
        return function() {
            if (rpc) {
                delete ug.disableRpc[rpc];
            }
            for (var i = 0; i < disabled.length; ++ i) {
                disabled[i]();
            }
        };
    },

    // set aria-busy attribute to true on specified elements
    setBusy: function(els, rpc) {
        var busy = [];

        var setBusy = function(elOrId, isBusy) {
            var element = Ext.get(elOrId);
            if (element) {
                element.set({ 'aria-busy': isBusy});
                if (isBusy) {
                    busy.push(elOrId);
                }
            }
        };

        if (els) {
            if (typeof(els) == 'string') {
                var list = els.split(',');
                for (var i = 0; i < list.length; i++) {
                    setBusy(list[i], true);
                }
            } else if (els.length) {
                for (var i = 0; i < els.length; i++) {
                    setBusy(els[i], true);
                }
            } else {
                setBusy(els, true);
            }
        }

        return function() {
            for (var i = 0; i < busy.length; i++) {
                setBusy(busy[i], false);
            }
        };
    },

    // Partial page reload
    ppr: function(data, params) {
        if (typeof(data) == 'string') {
            data = { render: data, params: params };
        }
        var conn = new ug.Connection();
        // strip off any query string for now..
        var href = document.location.href.replace(/[#?].*/, '');
        var undisable = ug.disable(data.disable);
        var setNotBusy = ug.setBusy(data.render);
        conn.request(
            {
                url: data.url || href,
                method: data.method || 'POST',
                params: data.params || {},
                timeout: data.timeout || 300000,
                headers: { 'com.sun.faces.avatar.Partial': true,
                           'com.sun.faces.avatar.Render': data.render },
                success: function(responseObject) {
                    // TODO: use the dynafaces js
                    var doc = responseObject.responseXML;
                    var partialResponseEl = doc.documentElement;
                    var componentsEl = partialResponseEl.firstChild;
                    for (var i = 0; i < componentsEl.childNodes.length; ++ i) {
                        var renderEl = componentsEl.childNodes[i];
                        var renderId = renderEl.getAttribute('id');
                        var markupEl = renderEl.firstChild;
                        var markupTxt = '';
                        for (var j = 0; j < markupEl.childNodes.length; ++ j) {
                            content = markupEl.childNodes[j];
                            markupTxt += content.text || content.data;
                        }
                        markupTxt = markupTxt.replace(/ *id=['"][^'"]*['"]/, '');
                        Ext.fly(renderId).update(markupTxt, true);
                        /* see UG-1162
                        var origEl = document.getElementById(renderId);
                        var temp = document.createElement('div');
                        temp.innerHTML = markupTxt;
                        var result = temp.firstChild;
                        var parent = origEl.parentNode;
                        parent.replaceChild(result, origEl); */
                    }
                    undisable();
                    setNotBusy();
                    if (data.callback) {
                        data.callback.call(data.scope);
                    }
                },
                failure: function() {
                    var retryCb = function() {
                        undisable(); // TODO: FIXME: don't do this...
                        setNotBusy();
                        ug.ppr(data);
                    };
                    if (!ug.leaving) {
                        var dialog = new ug.Dialog2({ title: "${msg.underground_network_error_title}",
                             body: "${msg.underground_network_error_message}",
                             defaultButton: 'okay',
                             okay: { label: "${msg.underground_network_error_action_retry}", handler: retryCb, autoHide: true },
                             cancel: { label: "${msg.underground_network_error_action_cancel}", handler: undisable, autoHide: true }
                        });
                        dialog.show();
                    }
                }
            });
    },

    Connection: function() {
        ug.Connection.superclass.constructor.apply(this, arguments);
        this.on({ beforerequest: function() { ug.loading(1, 'b4req'); },
                  requestcomplete: function() { ug.loading(-1, 'reqok'); },
                  requestexception: function() { ug.loading(-1, 'reqex'); } });
        this.defaultHeaders = { 'X-CSRF': 'true' };
    },

    loadingCount: 0,
    addOutId: 0,
    hideOutId: 0,

    loading: function(delta, from) {
        if (ug.parentWindowCall('loading', delta, from)) {
            return;
        }
        try {
            var loading = Ext.get('pageLayout-loading');
            if (loading) {
                ug.loadingCount = Math.max(0, ug.loadingCount + delta);
                if (ug.loadingCount > 0) {
                    loading.removeClass("hide");
                    clearTimeout(ug.addOutId);
                    clearTimeout(ug.hideOutId);
                    ug.addOutId = setTimeout(function(){
                        loading.addClass("out");
                    }, 0);
                } else {
                    loading.removeClass("out");
                    clearTimeout(ug.addOutId);
                    clearTimeout(ug.hideOutId);
                    ug.hideOutId = setTimeout(function(){
                        loading.addClass("hide");
                    }, 1000);
                }
            }
        } catch (ignored) {
            // throws during onsubmit handling
        }
    },

    yield: function(fn) {
        setTimeout(fn, 1);
    },

    wait: function(title, msg) {
        Ext.MessageBox.wait(msg, title);
    },

    doneWaiting: function() {
        Ext.MessageBox.hide();
    },

    alert: function(a, cfg) {
        if (ug.parentWindowCall('alert', a, cfg)) {
            return;
        }
        if (typeof(a) == 'string') {
            cfg = { title: a, message: cfg };
            a = null;
        }
        var dialog = new ug.Dialog2({
             title: cfg.title,
             body: cfg.message,
             width: cfg.width || 280,
             okay: { autoHide: (cfg.okay != ug.reload), handler: cfg.okay },
             destroyOnHide: true,
             defaultButton: 'okay',
             listeners: cfg.listeners
           });
        dialog.show();
        return dialog;
    },

    confirm: function(a, cf) {
//        if (ug.parentWindowCall('confirm', a, cf)) {
//            return;
//        }
        var dialog = new ug.Dialog2({
                 id: cf.id,
                 title: cf.title || "${msg.underground_confirm_title}",
                 width: cf.width || 280,
                 defaultButton: cf.defaultButton || 'okay',
                 body: Ext.util.Format.htmlEncode(cf.message || "${msg.underground_confirm_message}"),
                 okay: ug.button2(cf.okay, {label: "${msg.underground_confirm_action_ok}",  autoHide: true }),
                 okay2: cf.okay2 && ug.button2(cf.okay2, {label: "${msg.underground_confirm_action_ok}",  autoHide: true }),
                 cancel: ug.button2(cf.cancel, { label: "${msg.underground_confirm_action_cancel}", autoHide: true }),
                 destroyOnHide: true,
                 listeners: cf.listeners
               });
        dialog.show(a);
        return dialog;
    },

    showMsg: function(id, msg) {
        var el = Ext.getDom(id);
        if (msg) {
            el.innerHTML = msg;
            ug.displayEl(el);
        } else if (el)  {
            el.style.display = 'none';
        }
    },

    displayEl: function(el) {
        el.style.display = (el.nodeName == 'SPAN') ? 'inline' : 'block';
    },

    initForm: function(formRef, config, handler) {
        if (typeof config == 'function') {
            handler = config;
            config = {};
        } else {
            config = config || {};
        }
        var form = Ext.get(formRef);
        if (handler) {
            form.on('submit', function(e) {
                        e.preventDefault();
                        handler();
                    });
        }
        var formPrefix = form.dom.getAttribute('name') ? form.dom.name + ':' : '';
        // Copy the initial elements in case initialization causes new
        // fields to be created.
        var elements = [];
        for (var i = 0; i < form.dom.length; ++ i) {
            elements[i] = form.dom.elements[i];
        }
        // Use a function here because of freaky scoping problems
        // otherwise - modify podcast episode fails, field has no props on click.
        var initDepends = function(field, depends) {
            field.setDisabled(!depends.dom.checked);
            var enabledClick = function() {
                field.setDisabled(!depends.dom.checked);
                if (field.isXType('datefield')) {
                    field.setValue(depends.dom.checked ? new Date() : '');
                }
            };
            depends.on('click', enabledClick);
        };
        for (var i = 0; i < elements.length; ++ i) {
            var element = elements[i];
            var cf = config[element.name.substring(formPrefix.length)];
            var type = (cf && (typeof(cf) == 'string')) ? cf : (cf && cf.type) || element.type;
            var field = null;
            switch (type) {
            case 'date':
                field = new Ext.form.DateField({ format: "${msg.dateFormat_extjs}", applyTo: element });
                // ug.initDateField(element, cf.depends);
                break;
            case 'text':
                field = new Ext.form.TextField({ applyTo: element });
                break;
            case 'password':
                field = new Ext.form.TextField({ applyTo: element });
                break;
            case 'checkbox':
                // ext-js latest breaks these - they ignore the
                // existing checked state and don't respond to onchange
                // field = new Ext.form.Checkbox({ applyTo: element });
                break;
            case 'radio':
                // field = new Ext.form.Radio({ applyTo: element });
                break;
            case 'button': case 'submit':
                // new Ext.Button({ applyTo: element });
                break;
            }
            var depends = cf && Ext.get(formPrefix + cf.depends);
            if (field && depends) { // checkbox/radio only
                initDepends(field, depends);
            }
        }
    },

    initDateField: function(id, enabledRef, doDirty) { // TODO: kill me
      var field = new Ext.form.DateField({ format: "${msg.dateFormat_extjs}", applyTo: id, listeners: { 'select' : function() { ug.log('selected'); } }} );
      // field.setValue(value);
      if (enabledRef) {
          var enabled = Ext.get(enabledRef);
          field.setDisabled(!enabled.dom.checked);
          function enabledClick() {
              field.setDisabled(!enabled.dom.checked);
              field.setValue(enabled.dom.checked ? new Date() : '');
              if (doDirty) {
                  ug.setDirty();
              }
          }
          enabled.on('click', enabledClick);
      }
      if (doDirty) {
        field.on('change', ug.setDirty);
      }
      return field;
    },

    getScrollBarWidth: function(){
        if(!Ext.isReady){
            return 0;
        }

        var div = Ext.getBody().createChild('<div class="x-hide-offsets" style="width:100px;height:50px;overflow:hidden;"><div style="height:200px;"></div></div>'),
        child = div.child('div', true);
        var w1 = child.offsetWidth;
        div.setStyle('overflow', (Ext.isWebKit || Ext.isGecko) ? 'auto' : 'scroll');
        var w2 = child.offsetWidth;
        div.remove();
        scrollWidth = w1 - w2 + 2;

        return scrollWidth;
    },

    addAncestralOuterWidths: function(element) {
        if (element) {
            element = Ext.fly(element);
            var outerWidth = element.getFrameWidth("lr") + element.getMargins("lr");
            return outerWidth + ug.addAncestralOuterWidths(element.parent());
        }

        return 0;
    },

    maximalHeight: function(lm) {
        var availableH = de_bodyHeight();
        var innerMostH = Ext.fly("pageLayout-body-inner-most").getHeight();
        lm = (typeof(lm) == 'string') ? Ext.fly(lm) : lm;
        var oldH = lm.dom ? lm.getHeight() : lm.getSize().height;
        return availableH - innerMostH + oldH - 20;
    },

    //dispatcher: new Ext.util.Observable(),

    layoutStretchReduction: 0,

    layout: function() {
        if (!ug.initialized) {
            return;
        }

        if (ug.layoutMaximize || ug.layoutStretch) {
            var lm = ug.layoutMaximize;
            if (lm) {
                lm = (typeof(lm) == 'string') ? Ext.get(lm) : lm;
                var minHeight = lm.minHeight ? lm.minHeight: 0;
                lm.setHeight(Math.max(minHeight, ug.maximalHeight(lm)));
            }
            var ls = ug.layoutStretch;
            if (ls) {
                var newW = ug.stretchedWidth();
                var oldW = ls.dom ? ls.getWidth() : ls.getSize().width;
                if (newW != oldW) {
                    ls.setWidth(newW - ug.layoutStretchReduction);
                }
            }
        }

        ug.dispatcher.fireEvent("layout");
    },

    stretchedWidth: function() {
        return document.body.clientWidth - Ext.fly("pageLayout-body-inner-most").getFrameWidth('lr');
    },

    // extra parameters:
    //   autoLoadItem:
    //   okay:
    //   cancel:
    //   beforeShow:
    Dialog: function(cfg) {
        var items = cfg.items, buttons = [], self = this, defaultButton = cfg.defaultButton;
        var dialogButton = function(o, d) {
            var b = {}, t = typeof(o);
            if (t == 'object') {
                b = o;
            } else if (t == 'function') {
                b.handler = o;
            } else if (t == 'string') {
                b.text = o;
            }
            b = Ext.applyIf(b, d);
            if (b.autoHide) {
                var oh = b.handler;
                b.handler = function() {
                    self.hide();
                    if (oh) oh.apply(this, arguments);
                };
            }
            return b;
        };

        var okay = cfg.okay;
        if (okay) {
            if (!Ext.isArray(okay)) {
                okay = [ okay ];
            }
            for (var i = 0; i < okay.length; ++ i) {
                buttons.push(dialogButton(okay[i], { text: "${msg.action_ok}" }));
            }
        }

        if (cfg.cancel !== false) {
            buttons.push(dialogButton(cfg.cancel, { text: "${msg.action_cancel}",
                                                    autoHide: true }));
        }

        var modal = cfg.modal != false;
        if (defaultButton == 'cancel') {
            defaultButton = buttons.length - 1;
        } else if ((defaultButton == 'okay') || (defaultButton == 'okay0')) {
            defaultButton = 0;
        } else if (defaultButton == 'okay1') { // TODO: generical index support
            defaultButton = 1;
        }
        ug.Dialog.superclass.constructor.call
        (this, Ext.apply
         (cfg,
          {
              closeAction: 'hide',
              modal: modal,
              plain: true,
              resizable: cfg.resizable,
              stateful: false, // TODO: ignored, how to fix this?
              autoShow: true,
              items: items,
              defaultButton: defaultButton,
              buttons: buttons,
              buttonAlign: 'left',
              listeners: { 'beforeshow': function() { self.resetButtons(); self.enable(); },
                           'hide': function() { if (this.destroyOnHide) { this.destroy(); }
                                                if (this.onHideCallback) { this.onHideCallback(); } } }
          }));

        // TODO: this should actually return t/f depending on whether the
        // buttons are already disabled, so the actual form submission
        // can be blocked if the buttons are disabled - else someone could
        // conspire to click OK and then hit return in the form
        // TODO: make the Cancel button / closing the dialog actually abort
        // the original operation
        this.disableButtons = function() {
            for (var i = 0; i < this.buttons.length - 1; ++ i) {
                this.buttons[i].disable();
            }
        };
        this.resetButtons = function() {
            for (var i = 0; i < this.buttons.length; ++ i) {
                this.buttons[i].enable();
            }
        };
        // TODO: figure out how to add these functions this via ext.extend in-line with this
        // declaration...
    },

    button2: function(o, d) {
        var t = typeof(o), x = {};
        if (t == 'object') x = o;
        else if (t == 'function') x.handler = o;
        else if (t == 'string') x.label = o;
        return Ext.applyIf(x, d);
    },

    doubleConfirm: function(a, cf) {
        var reconfirm = function() {
            ug.confirm(a, cf.subsequent);
        };
        ug.confirm(a, Ext.apply(cf.initial, { okay: ug.button2(cf.initial.okay, { handler: reconfirm })}));
    },

    timeElapsed: function(val) {
        // TODO: adjust the scale to days/hours if appropriate
        var timeInMinutes = parseInt(val / 60 + 0.5);
        var hours = parseInt(timeInMinutes / 60);
        var minutes = timeInMinutes % 60;

        return String.format("${msg.underground_timeElapsed_hoursMinutes}", hours, minutes);
    },

    // cfg.sm
    SelectionAction: function(cfg) {
        var sm = cfg.sm || cfg.scope.getSelectionModel(); // hack
        ug.SelectionAction.superclass.constructor.call(this, Ext.apply({}, cfg,
            {
                disabled: !sm.hasSelection(),
                sm: sm
            }));
        sm.on('selectionchange', function() {
                                                this.setDisabled(!sm.hasSelection());
                                                if (cfg.onSelectionChange) {
                                                    cfg.onSelectionChange(this);
                                                }
                                            }, this);
        var oh = cfg.handler, os = cfg.scope; // hugly
        this.setHandler(function(action, e) {
                            var sel = sm.singleSelect ? sm.getSelected() : sm.getSelections();
                            if (sel) {
                                oh.apply(os || window, [ sel, e ]);
                            }
                        });
        if (cfg.dblclick) { // horrible hack.. This is gridtable-specific.
            // Could rewrite this as { triggers: [ { rowdblclick: xyz } ] so
            // it generically gets trigged on the specified events of the specified items
            cfg.dblclick.on('rowdblclick', function(grid, row, e) {
                                if (e.browserEvent.ctrlKey) {
                                    // ctrl normally deselects..
                                    sm.selectRow(row, true);
                                }
                                var sel = sm.singleSelect ? sm.getSelected() : sm.getSelections();
                                if (sel) {
                                    oh.apply(os || window, [ sel, e ]);
                                }
                            });
        }
    },

    animStyle: function(el, style, dst) {
        if (ug.animTask) {
            Ext.TaskMgr.stop(ug.animTask);
            delete ug.animTask;
        }
        if (el && parseInt(el.getStyle(style)) != dst) {
            ug.animTask = {
                run: function() {
                    var value = parseInt(el.getStyle(style)) || 0;
                    var delta = parseInt((((value < dst) ? (dst - value) : (value - dst)) + 1) / 2);
                    value = (value < dst) ? (value + delta) : (value - delta);
                    if (delta != 0) {
                        el.setStyle(style, value + 'px');
                    }
                    if (value == dst) {
                        Ext.TaskMgr.stop(ug.animTask);
                        delete ug.animTask;
                    }
                },
                interval: 20
            };
            Ext.TaskMgr.start(ug.animTask);
        }
    },

    escape: function(str) {
        return Ext.util.Format.htmlEncode(str);
    },

    unescape: function(str) {
        return Ext.util.Format.htmlDecode(str);
    },

    updater: function(id, escape, loadScripts, cb, scope) {
        return function(response) {
            ug.update(id, response, false, true/*escape, loadScripts*/);
            if (cb) cb.call(scope || window);
        }; // broken: ug.update.createDelegate(ug, [ id, escape, loadScripts ], 1);
    },

    update: function(id, content, escape, loadScripts) {
        ug.render(id, content, escape, loadScripts, function(n, h) { n.innerHTML  = h; });
    },

    append: function(id, content, escape, loadScripts) {
        ug.render(id, content, escape, loadScripts, Ext.DomHelper.append);
    },

    render: function(id, content, escape, loadScripts, fn) {
        var fly = Ext.fly(id);
        if (fly) {
            ug.renderz(fly, escape ? ug.escape(content) : content, loadScripts, fn);
            ug.initHyperlinks(fly.dom);
        }
    },

    renderz : function(el, html, loadScripts, fn, callback){
        if (!el.dom) {
            return el;
        }
        html = html || "";

        if(loadScripts !== true){
            fn(el.dom, html);
            if(typeof callback == 'function'){
                callback();
            }
            return el;
        }

        var id = Ext.id(),
            dom = el.dom;

        html += '<span id="' + id + '"></span>';

        Ext.lib.Event.onAvailable(id, function(){
            ug.exec(html);
            el = document.getElementById(id);
            if(el){Ext.removeNode(el);}
            if(typeof callback == 'function'){
                callback();
            }
        });

        fn(dom, html.replace(/(?:<script.*?>)((\n|\r|.)*?)(?:<\/script>)/ig, ""));

        return el;
    },

    exec: function(html) {
        var DOC = document,
        hd = DOC.getElementsByTagName("head")[0],
        re = /(?:<script([^>]*)?>)((\n|\r|.)*?)(?:<\/script>)/ig,
        srcRe = /\ssrc=([\'\"])(.*?)\1/i,
        typeRe = /\stype=([\'\"])(.*?)\1/i,
        match,
        attrs,
        code,
        srcMatch,
        typeMatch,
        el,
        s;

        while((match = re.exec(html))){
            attrs = match[1], code = match[2];
            srcMatch = attrs ? attrs.match(srcRe) : false;
            if(srcMatch && srcMatch[2]){
                var src = srcMatch[2];
                if (!ug.scripts[src]) {
                    ug.scripts[src] = true;
                    s = DOC.createElement("script");
                    s.src = src;
                    typeMatch = attrs.match(typeRe);
                    if(typeMatch && typeMatch[2]){
                        s.type = typeMatch[2];
                    }
                    hd.appendChild(s);
                }
            }else if(code && code.length > 0){
                ug.execScript(code);
            }
        }
    },

    execScript: function(code) {
        try {
            if(window.execScript) {
                window.execScript(code);
            } else {
                window.eval(code);
            }
        } catch (ex) {
            var trace = ug.getStackTrace(ex)().join('\n');
            ug.log(ex, trace, code);
        }
    },

    innerText: function(id, noTrim) {
        var dom = Ext.fly(id, '_internal').dom;
        var txt = dom.textContent || dom.innerText || '';
        return noTrim ? txt : Ext.util.Format.trim(txt);
    },

    getValue: function(id, noTrim) {
        var el = Ext.fly(id, '_internal');
        var val = el && el.dom.value;
        return (!val || noTrim) ? val : Ext.util.Format.trim(val);
    },

    setValue: function(id, value) {
        var el = Ext.fly(id, '_internal');
        if (el) {
            el.dom.value = value;
        }
    },

    setClass: function(id, cls, yes) {
        var el = Ext.fly(id, '_internal');
        if (el) {
            if (yes) {
                el.addClass(cls);
            } else {
                el.removeClass(cls);
            }
        }
    },

    scrollTo: function(y) {
        if (Ext.isWebKit) {
            document.body.scrollTop = y;
        } else {
            document.documentElement.scrollTop = y;
        }
    },

    // UG-1382: el.scrollintoView() is broken
    scrollIntoView: function(id) {
        var box = Ext.fly(id).getBox();
        box.bottom = box.y + box.height;
        var docEl = document.documentElement;
        var scrollOffset = parseInt(Ext.isWebKit ? document.body.scrollTop : document.documentElement.scrollTop, 10);
        var scrollHeight = document.documentElement.clientHeight;
        var scrollBottom = scrollOffset + scrollHeight;
        if ((box.y <= scrollOffset) && (box.bottom >= scrollBottom)) {
            // spans the screen so do nothing
        } else if ((box.y < scrollOffset) || (box.height > scrollHeight)) {
            ug.scrollTo(box.y);
        } else if (box.bottom > scrollBottom) {
            ug.scrollTo(box.bottom - scrollHeight);
        }
    },

    concatArrays: function(arrays) {
        var result = [];
        return result.concat.apply(result, arrays);
    },

    insertArray: function(dst, index, src) {
        var args = [ index, 0 ].concat(src);
        return dst.splice.apply(dst, args);
    },

    flyChild: function(cell, cls) {
        var el = Ext.fly(cell);
        return Ext.fly(el.child(cls, true));
    },

    radioClass: function(el, cls) {
        Ext.fly(el).radioClass(cls);
    },

    toDomainUrl: function(url) { // if the url is from the cpf host, strip the host and returns just /foo, else return the original url
        return (url && (url.indexOf(ug.host) == 0))
            ? url.substring(ug.host.length - 1) : url;
    },

    initSearch: function(context) {
        Ext.each(Ext.fly(context || document.body).query('.cp_emptySearch'), function(i) {
                   ug.initEmptyInput(i, i.value);
                 });
    },

    initEmptyInput: function(id, value) {
        var el = Ext.get(id);
        var onFocus = function() { if (el.hasClass('cp_emptyInput')) { el.dom.value = ''; el.removeClass('cp_emptyInput'); } };
        var onBlur = function() { if ((el.dom.value == '') || (el.dom.value == value)) { el.addClass('cp_emptyInput'); el.dom.value = value; } };
        var onChange = function() { if (el.hasClass('cp_emptyInput') && (el.dom.value != '') && (el.dom.value != value)) { el.removeClass('cp_emptyInput'); } };
        el.on({ focus: onFocus, blur: onBlur, change: onChange });
        onBlur();
    },

    initEmptyInputLabel: function(id, lid) {
        var el = Ext.get(id), label = Ext.get(lid);
        var onFocus = function() { ug.setClass(label, 'undisplayed', true); };
        var onBlur = function() { ug.setClass(label, 'undisplayed', el.dom.value != ''); };
        var onChange = onBlur;
        el.on({ focus: onFocus, blur: onBlur, change: onChange });
        label.on({ click: function() { el.focus(); } });
        onBlur();
    },

    initHyperlinks: function(el) {
        Ext.each(Ext.fly(el || document.body).query('.permacontent a'), function(a) {
                     var url = ug.toDomainUrl(a.href);
                     // ignore non-hyperlinks, external hyperlinks or same-page anchor hyperlinks.
                     if (url && (url.charAt(0) == '/') && (url.replace(/#.*/, '') != document.location.pathname)) {
                         Ext.EventManager.on(a, 'click', ug.onPermalink);
                     }
                 });
    },

    onPermalink: function() {
        // if a link within a permacontent is clicked then set the referrer
        // to be the permacontext permalink. thus, clicking from within a
        // blog entry or a wiki homepage will accurately record the url of
        // the context of that click.
        var permacontext = Ext.fly(this).parent('.permacontext');
        if (permacontext) {
            var permalink = permacontext.child('.permalink a', true);
            if (permalink && permalink.href) {
                ug.setCookie({ name: 'UGREF', value: permalink.href, expires: 1000 * 60 * 15 });
            }
        }
    },

    setCookie: function(o) {
        var cookie = o.name + '=' + escape(o.value || '');
        if (o.expires) {
            var expires = new Date();
            expires.setTime((o.expires > 0) ? (expires.getTime() + o.expires) : 0);
            cookie += ';expires=' + expires.toGMTString();
        }
        var host = document.location.host, match;
        if ((o.xdomain) && (match = host.match(/^[a-z][^.]*(\.[^:]*)(:.*)?$/))) {
            cookie += ';domain=' + match[1];
        }
        cookie += ';path=/';
        if (o.secure) {
            cookie += ';secure';
        }
        document.cookie = cookie;
    },

    getCookie: function(name) {
        return Ext.util.Cookies.get(name);
    },

    jsreport: function(ex, interactive, message) {
        var trace = message ? message + '\n' : '';
        try {
            trace = this.getStackTrace(ex)().join('\n');
            ug.log(ex, trace);
        } catch (ignored) {
        }
        var conn = new ug.Connection();
        conn.request(
            {
                url: '/control/javascriptException',
                method: 'POST',
                params: { exception: '' + ex,
                          trace: trace,
                          userAgent: navigator.userAgent,
                          url: document.location.href },
                timeout: 10000,
                success: function(response) {
                              if (interactive) { // Should I restrict it so?
                                  ug.error(null, { message: "${msg.underground_javascriptError}", guid: response.responseText });
                              }
                         }
            });
    },

    themeData: {
        first: true
    },

    getStackTrace : function (e) { // http://eriwen.com/javascript/js-stack-trace/
        var mode = e.stack ? 'Firefox' : window.opera ? 'Opera' : 'Other';

        switch (mode) {
            case 'Firefox' : return function () {
                return e.stack.replace(/^.*?\n/,'').
                replace(/(?:\n@:0)?\s+$/m,'').
                replace(/^\(/gm,'{anonymous}(').
                split("\n");
            };

            case 'Opera' : return function () {
                var lines = e.message.split("\n"),
                    ANON = '{anonymous}',
                    lineRE = /Line\s+(\d+).*?in\s+(http\S+)(?:.*?in\s+function\s+(\S+))?/i,
                    i,j,len;

                for (i=4,j=0,len=lines.length; i<len; i+=2) {
                    if (lineRE.test(lines[i])) {
                        lines[j++] = (RegExp.$3 ?
                            RegExp.$3 + '()@' + RegExp.$2 + RegExp.$1 :
                            ANON + RegExp.$2 + ':' + RegExp.$1) +
                            ' -- ' + lines[i+1].replace(/^\s+/,'');
                    }
                }

                lines.splice(j,lines.length-j);
                return lines;
            };

            default : return function () {
                var curr  = arguments.callee.caller,
                    FUNC  = 'function', ANON = "{anonymous}",
                    fnRE  = /function\s*([\w\-$]+)?\s*\(/i,
                    stack = [],j=0,
                    fn,args,i;

                while (curr) {
                    fn    = fnRE.test(curr.toString()) ? RegExp.$1 || ANON : ANON;
                    args  = stack.slice.call(curr.arguments);
                    i     = args.length;

                    while (i--) {
                        switch (typeof args[i]) {
                            case 'string'  : args[i] = '"'+args[i].replace(/"/g,'\\"')+'"'; break;
                            case 'function': args[i] = FUNC; break;
                        }
                    }

                    stack[j++] = fn + '(' + args.join() + ')';
                    curr = curr.caller;
                }

                return stack;
            };
        }
    },

    /**
     * Convenience method that reports whether this window is nested within an
     * iframe or frameset.
     */
    isFramed: function() {
        return parent !== window;
    },

    initSearchField: function(el, o, cb) {
        var searchStr = '', searchTimeout = null, minLength = o.minLength || 0, delay = o.delay || 500;
        $(el).bind('keydown keyup keypress change', function() {
            // If the search string has changed and either it is now blank, or has at
            // least min characters, or else I've already got the spinner spinning...
            if (((this.value != searchStr) && (!this.value || (this.value.length >= minLength))) || searchTimeout) {
                $(this).addClass('cp_waitIndicator');
                if (searchTimeout) {
                    clearTimeout(searchTimeout);
                }
                var me = this;
                searchTimeout = setTimeout(function() {
                    searchTimeout = null;
                    $(me).removeClass('cp_waitIndicator');
                    if (!me.value || (me.value.length >= minLength)) {
                        searchStr = me.value;
                        cb.call(me, searchStr);
                    }
                }, delay);
            }
        });
    },

    /**
     * This namespace includes common utilities for working with elements.
     */
    el: {
        /**
         * Finds the rendered height (in pixels) for the provided element, performing adjustments if necessary.
         *
         * @param {Object} [element] Either the unique identifier or any HTML element reference.
         *
         * @return {Number} The computed height, including borders, margins, and padding.
         */
        measureEffectiveHeight: function(element) {
            element = Ext.get(element);

            if (null == element) {
                return 0;
            }

            var height = 0;

            height += element.getComputedHeight(true);

            if (Ext.isIE6 || Ext.isIE7) {
                height += element.getMargins("tb");
            } else {
                /* Adjustment for every other browser. It is not clear whether the browsers report it or Ext JS calculates it incorrectly. */
                height += element.getMargins("tb") * 2;
            }

            height += element.getFrameWidth("tb");

            return height;
        },

        /**
         * Ascends the page model to the root, accumulating all contributors to vertical spacing.
         *
         * @param {Object} element The starting element.
         *
         * @return {Number} The total padding, frame, and margin height for all elements from the initial element to the document root.
         */
        measureAncestralOuterHeights: function(element) {
            element = Ext.get(element);

            if (null == element) {
                return 0;
            }

            var outerHeight = element.getFrameWidth("tb") + element.getMargins("tb");
            return outerHeight + ug.el.measureAncestralOuterHeights(element.parent());
        },

        /**
         * Calculates the visible region within the browser window.
         *
         * @return {Object} The width and height measurements specified in identically named properites.
         */
        measureViewport: function() {
            var viewportWidth;
            var viewportHeight;

            if (window.innerWidth) {
                /* Standards-compliant browsers. */
                viewportWidth = window.innerWidth;
                viewportHeight = window.innerHeight;
            } else if (document.documentElement && document.documentElement.clientWidth) {
                /* IE 6 in standards compliant mode (document type has been declared). */
                viewportWidth = document.documentElement.clientWidth;
                viewportHeight = document.documentElement.clientHeight;
            } else {
                /* Older IE versions in quirks mode. */
                viewportWidth = document.getElementsByTagName('body')[0].clientWidth;
                viewportHeight = document.getElementsByTagName('body')[0].clientHeight;
            }

            return {
                width: viewportWidth,
                height: viewportHeight
            };
        }
    },

    xss: {
        /**
         * This method will call window.postMessage if available, setting the targetOrigin parameter to the base of the target_url parameter for maximum security in browsers that support it. If window.postMessage is not available, the target window's location.hash will be used to pass the message.
         *
         * @param {String, Object} [message] A message to be passed to the other frame, which may be serialized using {Ext.encode}.
         *
         * @param {String} [target_url] The URL of the other frame this window is attempting to communicate with. This must be the exact URL (including any query string) of the other window for this script to work in browsers that don't support window.postMessage.
         *
         * @param {Object} [target] A reference to the other frame this window is attempting to communicate with. If omitted, defaults to the implicit parent object.
         */
        postMessage: function(message, target_url, target) {
            if (!target_url) { return; }

            /* Serialized mixed content. */
            message = Ext.isString(message) ? message : Ext.encode(message);

            /* Default to parent if unspecified. */
            target = target || parent;

            if (hasPostMessage) {
                /*
                 * The browser supports window.postMessage, so call it with a
                 * targetOrigin set appropriately, based on the target_url
                 * parameter.
                 */
                var targetLocation = target_url.replace(/([^:]+:\/\/[^\/]+).*/, "$1");
                target.postMessage(message, targetLocation);
            } else {
                /*
                 * The browser does not support window.postMessage, so set the
                 * location of the target to target_url#message. A bit ugly, but it
                 * works! A cache bust parameter is added to ensure that repeat
                 * messages trigger the callback.
                 */
                var updatedLocation = target_url.replace(/#.*$/, "") + "#" + (+new Date) + (postMessageCounter++) + '&' + message;
                target.location = updatedLocation;
            }
        },

        /**
         * Register a single callback for either a window.postMessage call, if supported, or if unsupported, for any change in the current window.location.hash. If window.postMessage is supported and source_origin is specified, the source window will be checked against this for maximum security. If window.postMessage is unsupported, a polling loop will be started to watch for changes to the location.hash.
         *
         * Note that for simplicity's sake, only a single callback can be registered at one time. Passing no params will unbind this event (or stop the polling loop), and calling this method a second time with another callback will unbind the event (or stop the polling loop) first, before binding the new callback.
         *
         * Also note that if window.postMessage is available, the optional source_origin param will be used to test the event.origin property. From the MDC window.postMessage docs: This string is the concatenation of the protocol and "://", the host name if one exists, and ":" followed by a port number if a port is present and differs from the default port for the given protocol. Examples of typical origins are https://example.org (implying port 443), http://example.net (implying port 80), and http://example.com:8080.
         *
         * @param {Function} [callback] This callback will execute whenever a ug.postMessage message is received, provided the source_origin matches. If callback is omitted, any existing receiveMessage event bind or polling loop will be canceled.
         *
         * @param {String, Function} [source_origin] If window.postMessage is available and this value is not equal to the event.origin property, the callback will not be called. Alternatively, if this function returns false when passed the event.origin property, the callback will not be called.
         *
         * @param {Number} [delay] An optional zero-or-greater delay in milliseconds at which the polling loop will execute (for browser that don't support window.postMessage). If omitted, defaults to 100.
         */
        receiveMessage: function(callback, source_origin, delay) {
            if (hasPostMessage) {
                /* Since the browser supports window.postMessage, the callback will be bound to the actual event associated with window.postMessage. */
                if (callback) {
                    /* Unbind an existing callback if it exists. */
                    /* TODO: Make this easier to read. */
                    ug.rm_callback && ug.receiveMessage();

                    /* Bind the callback. A reference to the callback is stored for ease of unbinding. */
                    ug.rm_callback = function(e) {
                        if ( ( typeof source_origin === 'string' && e.origin !== source_origin )
                                || ( Ext.isFunction( source_origin ) && source_origin( e.origin ) === FALSE ) ) {
                            return FALSE;
                        }
                        callback( e );
                    };
                }

                if ( window["addEventListener"] ) {
                    window[ callback ? "addEventListener" : 'removeEventListener' ]( 'message', ug.rm_callback, false );
                } else {
                    window[ callback ? 'attachEvent' : 'detachEvent' ]( 'onmessage', ug.rm_callback );
                }

            } else {
                /* The browser does not support postMessage. A polling loop will be started, and the callback will be called whenever the location.hash changes. */
                ug.interval_id && clearInterval(ug.interval_id);
                ug.interval_id = null;

                if (callback) {
                    delay = typeof source_origin === 'number'
                        ? source_origin
                                : typeof delay === 'number'
                                    ? delay
                                            : 100;

                    ug.interval_id = setInterval(function(){
                        var hash = document.location.hash,
                        re = /^#?\d+&/;
                        if ( hash !== ug.last_hash && re.test( hash ) ) {
                            ug.last_hash = hash;
                            callback({ data: hash.replace( re, '' ) });
                        }
                    }, delay );
                }
            }
        }
    },

    integration: {
        calculateContentHeight: function() {
            var height = 0;

            height += ug.el.measureEffectiveHeight("innerPage");
            height += ug.el.measureEffectiveHeight("pageLayout-footer");

            return height;
        },

        managedContentSizing: false,

        initializeContentSizing: function(contentWrapperURL, wrapperWindow) {
            if (ug.isFramed() && !Ext.isIE7) {
                ug.integration.managedContentSizing = true;

                var signalResize = function() {
                    /* Pages attempting to maximize certain elements do not need resized. Let the container size the nested content as necessary. */
                    if (ug.layoutMaximize) {
                        return;
                    }

                    ug.xss.postMessage({
                        height: ug.integration.calculateContentHeight()
                    }, contentWrapperURL, wrapperWindow);
                }

                Ext.onReady(function() {
                    Ext.fly(window).on({
                        resize: signalResize
                    });

                    ug.dispatcher.on({
                        layout: signalResize
                    });

                    signalResize();
                });
            }
        }
    },

    /**
     * Shows and hides success and failure messages of a given action
     *
     * @param messageContainerId The String ID of the HTML container to place the message
     * @param isSuccess boolean indicating that the action resulted in success or failure, true if success, false otherwise
     * @param successMessage String of the message to display upon successful actions
     * @param failureMessage String of the message to display upon failed actions
     */
    showStatus: function(messageContainerId, isSuccess, successMessage, failureMessage) {
        var el = Ext.fly(messageContainerId);
        el.update(isSuccess ? successMessage : failureMessage);
        el.replaceClass(isSuccess ? 'cp_incorrectResponse' : 'cp_correctResponse', isSuccess ? 'cp_correctResponse' : 'cp_incorrectResponse');
    },

    /**
     * Retrieves values from the current url.
     *
     * @param name The name of the parameter as it appears in the url
     */
    getUrlParameterByName: function(name) {
        name = name.replace(/[\[\]]/g, "\\$&");
        var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
            results = regex.exec(window.location.href);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, " "));
    }
};
