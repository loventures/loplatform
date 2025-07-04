var de = {};

de.asyncProgressBarHandler = function(id) {
    return function(progress) {
        var pb = $(id);
        if (progress.status == 'start') {
            pb.children('.bar').css('width', '0').removeClass('bar-success bar-danger');
            pb.children('.status').text('');
            pb.show();
        } else if (progress.status == 'progress') {
            var percent = parseInt(100 * progress.done / progress.todo);
            pb.children('.bar').css('width', percent + '%');
            pb.children('.status').text(progress.description);
        } else if (progress.status == 'ok') {
            pb.children('.bar').css('width', '100%').addClass('bar-success');
            pb.children('.status').text('');
        } else {
            pb.children('.bar').addClass('bar-danger');
            pb.children('.status').text('');
        }
    };
}

de.ajax = function(url, settings) {
    var q = new $.Deferred();
    var config = $.extend({}, settings, (typeof(url) == 'string') ? { 'url': url } : url);
    var asjax = function(response) {
        q.notify({ status: 'start' });
        var channel = response.channel;
        var msgs = new EventSource("/event" + channel);
        var timestamp = new Date(0);
        msgs.addEventListener(channel, function (event) {
            var data = $.parseJSON(event.data), body = data.body;
            if (data.status == 'ok') {
                msgs.close();
                q.notify({ status: 'ok' });
                q.resolve(body, 'async-ok', event);
            } else if (data.status == 'progress') {
                var eventDate = new Date(data.timestamp);
                if (eventDate > timestamp) {
                    timestamp = eventDate;
                    q.notify({ status: 'progress', done: body.done,
                               todo: body.todo, description: body.description }, body);
                }
            } else if (data.status == 'warning') {
                q.notify({ status: 'warning', message: body.message }, body);
            } else { // error
                msgs.close();
                q.reject(event, 'async-error', body);
            }
        });
    };
    var lojax = function() {
        $.ajax(config)
            .done(function(response, text, jqxhr) {
                if (jqxhr.status == 202) {
                    if (response.status == 'async') {
                        asjax(response);
                    } else if ((response.status == 'challenge') &&
                               (!config.headers || !config.headers['X-Challenge-Response'])) {
                        var response = rstr2b64(rstr_sha1(str2rstr_utf8(response.challenge)));
                        config.headers = $.extend({}, config.headers, { 'X-Challenge-Response': response });
                        lojax();
                    } else {
                        q.reject(jqxhr, 'accept-error', response);
                    }
                } else {
                    q.resolve(response, text, jqxhr);
                }
            })
            .fail(function(jqxhr, status, error) {
                q.reject(jqxhr, status, error); // args..
            });
    };
    lojax();
    return q.promise();
};

/**
 * matrix.offset, matrix.limit,
 * matrix.order: 'prop:asc' | { property: 'prop', direction: 'asc' } | array of same
 * matrix.filter: 'prop:op(val)' | { property: 'prop', operator: 'op', value: 'val' } | array of same
 * matrix.prefilter: 'prop:op(val)' | { property: 'prop', operator: 'op', value: 'val' } | array of same
 */
de.encodeQuery = function(offset, limit, order, filter, prefilter) {
    var matrix = (typeof(offset) == 'object') ? offset
        : { offset: offset, limit: limit, order: order, filter: filter, prefilter: prefilter };
    var encodeValue = function(value) {
        return encodeURIComponent(value).replace(/\)/g, "%29");
    };
    return $.map(matrix, function(v, k) {
        var encoded;
        if (v && (k == 'order')) {
            encoded = $.map($.isArray(v) ? v : [ v ], function(o) {
                return (typeof(o) == 'string') ? o : (o.property + ':' + o.direction);
            }).join(',');
        } else if (v && ((k == 'filter') || (k == 'prefilter'))) {
            encoded = $.map($.isArray(v) ? v : [ v ], function(o) {
                return (typeof(o) == 'string') ? o : (o.property + (o.operator ? ':' + o.operator : '') + '(' + encodeValue(o.value) + ')');
            }).join(',');
        } else if (v && (k == 'embed')) {
            encoded = ($.isArray(v) ? v : [ v ]).join(',');
        } else {
            encoded = v;
        }
        return encoded ? (k + '=' + encoded) : null;
    }).join(';');
};

de.patch = function(url, data) {
    var patch = $.map(data, function(v, k) {
        return { op: 'add', path: '/' + k, value: v };
    });
    return de.ajax(url,
                   { type: 'PATCH',
                     contentType: "application/json-patch+json;charset=utf-8",
                     data: JSON.stringify(patch),
                     processData: false });
};

de.formatBytes = function(bytes) { // TODO: i18n
    var sizes = ['bytes', 'KB', 'MB', 'GB', 'TB'];
    if (bytes == 0) return '0 Bytes';
    var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
    return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];
};
