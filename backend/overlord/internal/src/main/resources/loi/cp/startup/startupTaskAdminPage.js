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

$(function() {

  // To be clear, this code is embarrassing. But now is not the time to
  // bring front-end frameworks to overlord.

  $(document).ajaxError(function(event, jqxhr, settings, error) {
    alert('An error occurred: ' + jqxhr.responseText);
  });

  function startProgress() {
    $('#status-content').empty();
    $('#main').addClass('consoleOpen');
    fixHeight();
  }

    function showProgress(status) {
        _.each(status.split('\n'), function(st) {
            $('<div>', { text: st }).appendTo($('#status-content'));
        });
        $('#status')[0].scrollTop = $('#status-content')[0].offsetHeight;
    }

    function progressCb(progress) {
        if (progress.status == 'progress') {
            showProgress(progress.description);
        }
    }

  function startupGroup(o) {
    var grp = $('<div>', { 'class': 'task-group' }).appendTo('#data-body');
    var tit = $('<div>', { 'class': 'title' }).appendTo(grp);
    var chev = $('<span>', { 'class': 'chevron' }).appendTo(tit);
    tit.on('dblclick', function(){
        grp.toggleClass('open');
    });
    chev.on('click', function() {
      grp.toggleClass('open');
    });
    var id = o.response.system ? 'status-system' : 'status-' + o.response.domain;
    $('<span>', { 'class': 'status ' + o.status, id: id, html: '&#x25C9;' }).appendTo(tit);
    $('<span>', { 'class': 'name', text: o.name }).appendTo(tit);
    var tasks = $('<div>', { 'class': 'tasks' }).appendTo(grp);
    _.each(o.tasks, function(t) {
        var task = $('<div>', { 'class': 'task' + (t.state ? '' : ' None') }).appendTo(tasks);
        var tit = $('<div>', { 'class': 'title' }).appendTo(task);
        var chevron = $('<span>', { 'class': 'chevron' }).appendTo(tit);
        var led = $('<span>', { 'class': 'status ' + t.state, id: id + '-' + t.identifier.replace(/\./g, '_'), html: '&#x25C9;' }).appendTo(tit);
        var name = t.identifier; // + ' (v.' + t.version + (t.upgrade ? ', upgrade)' : ')');
        $('<span>', { 'class': 'name', text: name }).appendTo(tit);
        var taskstuff =  $('<div>', { 'class': 'stuff' }).appendTo(task);
        var info = $('<div>', { 'class': 'info', text: t.state }).appendTo(taskstuff);
        var logs = $('<pre>', { 'class': 'logs' }).appendTo(taskstuff);
        var btns = $('<div>', { 'class': 'buttons' }).appendTo(taskstuff);
        var receipt = 0;
        function stateChange(state) {
          if (receipt) {
            $.ajax('/api/v2/startupTasks/receipts/' + receipt,
                   { type: 'PUT', contentType: 'application/json',
                     data: JSON.stringify({ state: state }), processData: false }).done(function(o) {
                // omg model view
                led.removeClass(info.text()).addClass(state);
                info.text(state);
                btns.children('.changed').toggle(state != t.state);
                btns.children('.change').toggle(state == t.state);
              });
          }
        }
        if (t.state != 'Retry') {
          var retry = $('<a>', { 'class': 'btn btn-warning change', text: 'Retry' }).appendTo(btns);
          retry.on('click', function() {
            stateChange('Retry');
          });
        }
        if (t.state == 'Failure') {
          var skip = $('<a>', { 'class': 'btn btn-warning change', text: 'Skip' }).appendTo(btns);
          skip.on('click', function() {
            stateChange('Skip');
          });
        }
        var undo = $('<a>', { 'class': 'btn btn-warning changed', text: 'Undo' }).appendTo(btns).hide();
        undo.on('click', function() {
          stateChange(t.state);
        });
        if (t.state) {
            var open = function(evt) {
              if (task.is('.open')) {
                task.removeClass('open');
              } else {
                var did = o.response.system ? [] : [ { property: 'root_id', operator: 'eq', value: o.response.domain } ];
                task.addClass('open');
                  var q = de.encodeQuery({
                    offset: 0, limit: 1,
                    order: { property: 'timestamp', direction: 'desc' },
                    prefilter: did.concat([
                      { property: 'identifier', operator: 'eq', value: t.identifier },
                      { property: 'version', operator: 'eq', value: t.version }
                    ]),
                    embed: 'logs'
                  });
                  $.get('/api/v2/startupTasks/receipts;' + q).done(function(o) {
                    if (o.count) {
                      var a = o.objects[0];
                      receipt = a.id;
                      logs.text(a.logs);
                    }
                  });
              }
            };
            chevron.on('click', open);
            tit.on('dblclick', open);
        }
    });
  }

  function processSummary(o) {
    var allGood = !_.find(o.tasks, function(t) { return (t.state != 'Success') && (t.state != 'Skip'); });
    var anyBad = !!_.find(o.tasks, function(t) { return t.state == 'Failure'; });
    var status = anyBad ? 'Failure' : allGood ? 'Success' : 'Skip';
    var name = o.system ? 'System Startup Tasks' : 'Domain Startup Tasks: ' + o.domainName + ' (' + o.domainId + ')';
    startupGroup({ name: name, status: status, tasks: o.tasks, response: o });
  }

  // This runs deliberately serially because each domain can be slow to retrieve,
  // throwing everything in one request is unbearable

  function processDomains(a, id) {
    if (id < a.length) {
      var d = a[id];
      function processDomain(p) {
        processSummary({ system: false, domain: d.id, domainId: d.domainId, domainName: d.name, tasks: p.objects });
        processDomains(a, 1 + id);
      }
      $.get('/api/v2/startupTasks/byDomain/' + d.id).done(processDomain);
    } else {
      $('#wait').hide();
    }
  }

  function processSystem(o) {
    processSummary({ system: true, tasks: o.objects });
    var ordinals = { Normal: 0, ReadOnly: 1, Maintenance: 2, Suspended: 3 };
    function processDomainList(o) {
      var domains = o.objects.sort(function(a, b) { // duplicate the ordering of StartupTaskService
        if (a.state == b.state) {
          return (a.name < b.name) ? -1 : (a.name > b.name) ? 1 : 0;
        } else {
          return ordinals[a.state] - ordinals[b.state];
        }
      });
      function processOverlordDomain(d) {
        processDomains([d].concat(domains), 0);
      }
      $.get('/api/v2/domain').done(processOverlordDomain);
    }
    $.get('/api/v2/domains;order=name:asc').done(processDomainList);
  }

  $('#action-latest').click(function(e) {
    e.preventDefault();
    var q = de.encodeQuery({
      offset: 0, limit: 1,
      order: { property: 'timestamp', direction: 'desc' },
      prefilter: [
        { property: 'identifier', operator: 'eq', value: 'system' }
      ]
    });
    $.get('/api/v2/startupTasks/receipts;' + q).done(function(o) {
      if (o.count) {
        var a = o.objects[0];
        window.open('/api/v2/startupTasks/receipts/' + a.id + '/logs/download', '_blank');
      }
    });
  });

  $.get('/api/v2/startupTasks/system').done(processSystem);

  $('#action-execute').click(function() {
      startProgress();
      de.ajax('/api/v2/startupTasks/execute', { type: 'POST' }).done(function() {
        showProgress('Done');
      }).progress(progressCb).fail(function(jq, st, error) {
        showProgress('Error: ' + error.detail);
      });
  });

  $('#close-execute').click(function() {
       $('#main').removeClass('consoleOpen');
       fixHeight();
    });

  // This shames me. If only there was such a thing as a javascript UI framework...

  var classes = 'Skip Success Retry Failure';
  var eventsUrl = '/api/v2/startupTasks/events';
  var es = new window.EventSource(eventsUrl);
  es.addEventListener('control', function(event) {
    if (event.data == 'stop') {
      es.close();
      $('#sub').text('Offline').attr("class", "label label-warning");
    }
  });
  es.onerror = function() { $('#sub').text('Error').attr("class", "label label-important"); }
  es.onopen = function() { $('#sub').text('Online').attr("class", "label"); }
  es.addEventListener('startup', function(event) {
    var type = event.type;
    var data = JSON.parse(event.data);
    if (data.status) {
      var id = '#status-' + (data.status.domain || 'system')
      var tid = 'thread-' + data.status.threadId;
      $('.' + tid).removeClass('throbbing');
      if (data.status.task.identifier != 'completion') {
        $(id).addClass('throbbing ' + tid);
      }
      $('.task .' + tid).removeClass('throbbing');
      var taskId = id + '-' + data.status.task.identifier.replace(/\./g, '_');
      $(taskId).removeClass(classes).addClass('throbbing ' + tid + ' ' + (data.status.state || ''));
      var statuses = $(id).parents('.task-group').find('.task .status');
      var anyBad = statuses.is('.Failure');
      var allGood = statuses.filter('.Success,.Skip').length == statuses.length;
      var status = anyBad ? 'Failure' : allGood ? 'Success' : 'Skip';
      if (!$(id).is('.' + status)) {
        $(id).removeClass(classes).addClass(status);
      }
      $('#sub').text('Active').attr("class", "label label-success");
    } else {
      $('.throbbing').removeClass('throbbing');
      $('#sub').text('Idle').attr("class", "label label-info");
    }
  });
  $(window).on('beforeunload', function() {
    if (es.readyState != 2) {
      $.ajax({ type: 'DELETE', url: eventsUrl, async: false });
    }
  });

    //loMaximize-esque quick fix - manually expand the editor and output areas to window size
    function fixHeight() {
        var multiplier = $("#main").hasClass("consoleOpen") ? 0.75 : 1;
        $("#data-body").height($(window).height() * multiplier - 128);
    };
    $(window).on("resize", fixHeight).trigger("resize");

});
