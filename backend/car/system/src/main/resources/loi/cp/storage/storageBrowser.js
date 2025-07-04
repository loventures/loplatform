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

// I am not pretty. I am not planned or designed. I am not well written. I evolved this way.
// I humbly apologize.

function selectRange(node, collapse) {
    var range = document.createRange();
    range.selectNodeContents(node);
    if (collapse) range.collapse(false);
    var selection = window.getSelection();
    selection.removeAllRanges();
    selection.addRange(range);
}

function setKeyboardSupport(on) {
    $('#storage').tree('setOption', 'keyboardSupport', on);
}

function goTo(id) {
  var tree = $('#storage')
  var node = tree.tree('getNodeById', id);
  if (node) {
    tree.tree('selectNode', node);
  } else {
    searchFor(id);
  }
}

function searchFor(text) {
    var tree = $('#storage'), filter = $('#filter');
    filter.addClass('searching');
    $.get(rpcBase + "getPath", { node: text }).then(function(d) {
        if (!d.length) {
            alert('Search returned no results.');
        } else {
            $.each(d, function(i, v) {
                var node = tree.tree('getNodeById', v.id);
                if (v.data) {
                    displayNodeData(node, v.data);
                    tree.tree('selectNode', node);
                    setTimeout(function() {
                        tree.tree('scrollToNode', node);
                    }, 50); // animation delay
                } else {
                    node.load_on_demand = false;
                    $.each(v.children, function(j, c) {
                        if (!tree.tree('getNodeById', c.id)) {
                            tree.tree('appendNode', c, node);
                        }
                    });
                    tree.tree('openNode', node);
                }
            });
            filter.val('').blur();
        }
    }, function() {
        window.alert('Something went horribly wrong.');
    }).always(function() {
        filter.removeClass('searching');
    });
}

var selection = null;
var selectionHistory = [];
var historyIndex = -1;

function displayNodeData(node, dat) {
    var tree = $('#storage'), filter = $('#filter');

    selection = node;
    addToHistory(node);

    clearDataPanel();

    $('#action-download').toggleClass('disabled', !node || (node.type == 'Domain'));
    $('#action-delete').toggleClass('disabled', !node || (node.type == 'Domain'));
    $('#action-attachment').toggleClass('disabled', !node || (node.type != 'Attachment'));

    if (!node || node.disabled) return;

    renderDataPanel(node, dat)
}

function addToHistory(node) {
    if (node !== null && node.id !== selectionHistory[historyIndex]) {
        selectionHistory.splice(historyIndex+1); // Chop off anything after the current index
        selectionHistory.push(node.id);
        historyIndex++;
    }
}

function clearDataPanel() {
    $('#data-head').text('-');
    $('#data-add').css('display', 'none');
    $('#data-add input').val('');
    $('#data-rows').empty();
}

function renderDataPanel(node, data) {
    var datatable = $('#data-rows');

    $('#data-head').text(node.id.replace(':', ' '));

    var rows = $.map(data, function(v, k) { return { type: k, value: v }; });
    rows.sort(function(o0, o1) { return o0.type < o1.type ? -1 : 1; });

    //Render rows of data
    $.each(rows, function(i, e) {
        var div = $('<div>', { 'class': 'data-row' }).appendTo(datatable);
        $('<span>', { text: e.type, 'class': 'data-key' }).appendTo(div);
        var span = $('<span>', { 'class': 'data-value', 'data-type': e.type }).appendTo(div);
        var text = e.value || '';
        var re = /\b([A-Za-z_]+:-?[0-9]+)/;
        $.each(text.toString().split(re), function(j, v) {
            if (re.test(v)) {
                $('<a>', { href: 'javascript:goTo("' + v + '")', text: v }).appendTo(span);
            } else {
                $('<span>', { text: v }).appendTo(span);
            }
        });
        var killa = $('<a>', { 'class': 'data-kill', html: '&times;', href: '#' })
          .appendTo(div)
          .click(function() {
            confirmClearDatum(e.type);
          });
    });

    //Render '+' button (for adding new data)
    $('<a>', { 'style': 'color: gray; margin-left: 4px', text: '+', href: '#' })
        .appendTo(datatable)
        .click(function() {
            $(this).css('display', 'none');
            $('#data-add').css('display', 'block')[0].scrollIntoView();
            $('#add-type').focus();
            return false;
        });
}

function confirmClearDatum(dt) {
    function clearDatum(dt) {
        return $.post(rpcBase + 'clearData', { node: selection.id, jsonModel: selection.jsonModel, type: dt })
            .fail(function() {
                window.alert('Something went horribly wrong.');
            });
    }
    if (window.confirm('Really clear ' + dt + ' on ' + selection.id + '?')) {
        return clearDatum(dt).then(function () {
            var sel = selection;
            queryNodeData(null);
            queryNodeData(sel);
            return true;
        });
    } else {
        el.blur();
        return $.Deferred().resolve(false);
    }
}

$(function() {
    var tree = $('#storage');
    var filter = $('#filter')
    var prevHistory = $('#prev-history');
    var nextHistory = $('#next-history');

    var datatable = $('#data-rows');

    var jsonmirror = CodeMirror.fromTextArea($('textarea#jsonmirror')[0], { mode: 'javascript', json: true });
    var jsonmirrorModal = $('div#modal-jsonmirror');

    // upon clicking on a data row, select the text
    datatable.on('click', 'div.data-row', function() {
        var child = $(this).find('.data-value');
        $('div.data-row.data-sel').removeClass('data-sel');
        $(this).addClass('data-sel');
        if (!child.attr('contenteditable')) {
            selectRange(child[0]);
        }
    });

    // upon double clicking on a data row, edit it
    datatable.on('dblclick', 'div.data-row', function() {
        var child = $(this).find('.data-value'), dt = $(this).find('.data-key').text();
        if (!child.attr('contenteditable')) {
            if (window.jsonTypes.indexOf(dt) >= 0) {
                jsonmirrorModal.data('datatype', dt);
                jsonmirrorModal.find('#modal-jsonmirror-datatype').text(dt);
                jsonmirrorModal.find('#modal-jsonmirror-item').text(selection.id);
                jsonmirrorModal.modal('show');
                jsonmirror.setValue(child.text());
                jsonmirror.markClean();
            } else {
                child.attr('contenteditable', true)
                    .addClass('editable')
                    .data('original', child.text())
                    .focus();
                selectRange(child[0], true);
                setKeyboardSupport(false);
            }
        }
    });

    datatable.on('blur', '.data-value.editable', function() {
        // technically this loses the original hyperlinks...
        $(this)
            .text($(this).data('original'))
            .attr('contenteditable', null)
            .removeClass('editable');
        setKeyboardSupport(true);
    });

    function setDatum(dt, dv, set) {
        return $.post(rpcBase + 'setData', { node: selection.id, jsonModel: selection.jsonModel, type: dt, value: dv, set: set })
            .fail(function() {
                window.alert('Something went horribly wrong.');
            });
    }

    var RETURN = 13;
    var ESCAPE = 27;
    var LEFT_ARROW = 37;
    var RIGHT_ARROW = 39;

    prevHistory.on('click', selectPreviousNode);
    nextHistory.on('click', selectNextNode);

    $(document).keydown(function(e) {
        if (e.ctrlKey && e.altKey && e.keyCode == LEFT_ARROW) {
            selectPreviousNode();
            e.nativeEvent.stopImmediatePropagation();
        } else if (e.ctrlKey && e.altKey && e.keyCode == RIGHT_ARROW) {
            selectNextNode();
            e.nativeEvent.stopImmediatePropagation();
        }
    });

    datatable.on('keydown', '.data-value.editable', function(e) {
        var el = $(this), dt = el.data('type'), dv = el.text();
        if(e.keyCode == RETURN) {
            e.preventDefault();
            setTimeout(function() {
                confirmSetDatum(dt, dv, el);
            }, 1);
        } else if (e.keyCode == ESCAPE) {
            el.blur();
        }
    });

    filter.on('keydown', function(e) {
        var text = filter.val();
        if ((e.keyCode == RETURN) && (text != '')) {
            searchFor(text);
        }
    });

    $('#add-value,#add-type').on('keydown', function(e) {
        if (e.keyCode == RETURN) {
          $('#do-add').click();
        } else if (e.keyCode == ESCAPE) {
          $('#data-add').css('display', 'none'); // TODO: add back the +
        }
    });
    $('#do-add').click(function() {
        var dt = $('#add-type').val(), dv = $('#add-value').val();
        if ((dt != '') && (dv != '') && selection) {
            setDatum(dt, dv, false).then(function() {
               var sel = selection;
               queryNodeData(null);
               queryNodeData(sel);
            });
        }
    });

    function resizeDataPanel() {
        var dataDiv = $("#data-panel");
        var marginWidth = dataDiv.outerWidth(true) - dataDiv.outerWidth() + 1; //Includes crucial fudge factor pixel
        dataDiv.width($("#bodies").width() - ($("#query-panel").width() + marginWidth));
    }

    $(window).on('resize', function() {
        tree.height($(window).height() - 76 - 40 );
        $('#data-body').height($(window).height() - 76 - 21 );
        resizeDataPanel();
    }).trigger('resize');

    //Application will behave like normal bootstrap spans until a resize occurs. At that point, this logic takes over.
    $("#query-panel").resizable({
        handles: "e"
    }).on("resize", resizeDataPanel);

    $('#action-delete').click(function(e) {
        if (selection && window.confirm("Really delete " + selection.name + "?")) {
            $('#modal-wait').modal('show');
            $.post(rpcBase + 'deleteNode', { node: selection.id,
                                             jsonModel: selection.jsonModel }).then(function() {
                tree.tree('removeNode', selection);
                displayNodeData(null, null);
                // ok, at this point coding ui without real model is just tedious
                // TODO: learn reactjs
            }, function() {
                window.alert('Something went horribly wrong.');
            }).always(function() {
                $('#modal-wait').modal('hide');
            });
        }
    });

    $('#action-download').click(function(e) {
        if (selection && !$(this).is('.disabled')) {
            window.frames.downloadFrame.location.href = rpcBase + 'downloadItem?node=' + selection.id + '&jsonModel=' + encodeURIComponent(selection.jsonModel || '');
            $(this).addClass('disabled');
        }
    });

    $('#action-attachment').click(function(e) {
        if (selection && !$(this).is('.disabled')) {
            window.frames.downloadFrame.location.href = rpcBase + 'downloadAttachment?node=' + selection.id + '&jsonModel=' + encodeURIComponent(selection.jsonModel || '');
            $(this).addClass('disabled');
        }
    });

    function doubleClick(node) {
        if (node.loadMore) {
            $(node.element).addClass('jqtree-loading');
            $.get(rpcBase + 'getChildren', { node: node.parent.id, offset: node.count }).then(function(data) {
                $.each(data, function(i, v) {
                    if (!tree.tree('getNodeById', v.id)) {
                        tree.tree('addNodeBefore', v, node);
                    }
                });
                tree.tree('removeNode', node);
                if (data.length) {
                    var first = tree.tree('getNodeById', data[0].id);
                    queryNodeData(first);
                    tree.tree('selectNode', first);
                } else {
                    displayNodeData(null, null);
                }
            }).always(function() {
              $(node.element).removeClass('jqtree-loading');
            });
        } else {
            queryNodeData(node);
            tree.tree('toggle', node);
            tree.tree('selectNode', node);
        }
    }

    $.get(rpcBase + 'getChildren', { node: 'root' }).then(function(data) {
        tree.tree({
            data: data,
            autoOpen: isOverlord ? false : 0,
            useContextMenu: false,
            onCreateLi: function(node, $li) {
                $li.addClass('node-type-' + node.type);
                var title = $li.find('.jqtree-title');
                title.text(node.name.replace(new RegExp("^" + node.type + "\:"), ""));
                title.html(title.html().replace(/^(-?\d+)/, '<span class="item-id">$1</span>'));
                title.prepend("<span class='item-type'>" + node.type + "</span>");

            }
        })
            .bind('tree.select', onSelect)
            .bind('tree.dblclick', function(event) {
                event.preventDefault();
                doubleClick(event.node);
            });
        // Work around rotten internal implementation of scrollToNode...
        var widget = tree.data('simple_widget_tree');
        widget.scrollToNode = function(node) {
            node.element.scrollIntoView();
            return this.element;
        }
        // Hijack open node to allow keyboard load-more. Adding support for enter is much harder.
        var oldOpen = $.proxy(widget.openNode, widget);
        widget.openNode = function(node) {
            if (node.loadMore) {
                doubleClick(node);
            } else {
                return oldOpen(node);
            }
        };
        var oldClose = $.proxy(widget.closeNode, widget);
        widget.closeNode = function(node) {
            oldClose(node);
            if (node.jsonModel) return; // don't unload transients
            node.load_on_demand = true;
            var children = $.map(node.children, function(v) { return tree.tree('getNodeById', v.id); });
            $.each(children, function(i, v) {
                tree.tree('removeNode', v);
            });
        };
    });

    function onSelect(event){
        queryNodeData(event.node);
    }

    window.queryNodeData = function(node){ // ahoist me hearties
        if (selection == node) return;
        displayNodeData(null, null);
        if (!node || node.disabled || node.type == 'Finder') return;
        if (node.data) {
            displayNodeData(node, node.data);
            return;
        }
        $.get(rpcBase + 'getData', { node:  node.id }).then(function(dat) {
            displayNodeData(node, dat);
        });
    }

    function selectPreviousNode() {
        if (historyIndex > 0) {
            historyIndex--;
        }
        searchFor(selectionHistory[historyIndex]);
    }

    function selectNextNode() {
        if (historyIndex < selectionHistory.length - 1) {
            historyIndex++;
        }
        searchFor(selectionHistory[historyIndex]);
    }

    function confirmSetDatum(dt, dv, el) {
        if (window.confirm('Really update ' + dt + ' on ' + selection.id + '?')) {
            return setDatum(dt, dv, true).then(function () {
                el.data('original', dv).text(dv);
                el.blur();
                return true;
            });
        } else {
            el.blur();
            return $.Deferred().resolve(false);
        }
    }

    $('#exit-button').click(function() {
        doExit()
            .done(function() {
                document.location.reload();
            });
        return false;
    });
    $('#logout-button').click(function() {
        doLogout()
            .done(function() {
                document.location.href = '/';
            });
        return false;
    });

    jsonmirrorModal.find('button[type=submit]').click(function () {
        var dt = jsonmirrorModal.data('datatype');
        var dv = jsonmirror.getValue('');
        var el = $('div.data-row.data-sel .data-value');
        confirmSetDatum(dt, dv, el).then(function (confirmed) {
            if (confirmed) {
                jsonmirrorModal.modal('hide');
            }
        })
    });

    jsonmirrorModal.find('button[type=reset]').click(function () {
        if (jsonmirror.isClean() || window.confirm('Really discard changes?')) {
            jsonmirrorModal.modal('hide');
        }
    })

});

$.ajaxSetup({ headers: { 'X-CSRF': 'true' } });
