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
    var HEIGHT_HACK = 184; // hacky, could be more accurate
    var selectedRow = null;

    $(document).ajaxError(function(event, jqxhr, settings, error) {
        alert('An error occurred: ' + jqxhr.responseText);
    });

    function renderUser(u) {
        return u.givenName + (u.middleName ? ' ' + u.middleName : '') + ' ' + u.familyName + (u.emailAddress ? ' <' + u.emailAddress + '>' : '') + ' (' + u.userName + ')';
    }

    var table;

    function initTable() {
        table = $("#dataTable").dataTable({
            "sDom": "<'row-fluid'<'#toolbar-container'>r>tS<'row-fluid'<'span12'i><'span12'>>",
            "fnServerData": function (sSource, aoData, fnCallback, oSettings) {
                var params = {};
                $.each(aoData, function(i, o) { params[o.name] = o.value; });
                var offset = params.start;
                var limit = Math.min(256, params.length);
                var order = { property: params.columns[params.order[0].column].data, direction: params.order[0].dir };
                var search = params.search.value;
                var filter = search ? { property: 'trashId', operator: 'eq', value: search } : null;
                $.get('/api/v2/overlord/trashRecords;embed=creator;' + de.encodeQuery(offset, limit, order, filter))
                    .done(function(data) {
                        fnCallback({ sEcho: params.sEcho,
                                     iTotalRecords: data.totalCount,
                                     iTotalDisplayRecords: data.filterCount,
                                     data: data.objects });
                    });
            },
            "sAjaxDataProp": "data",
            "bServerSide": true,
            "bDeferRender": true,
            "bStateSave": false, // seems kinda broken with Scroller
            "bAutoWidth": false,
            "sScrollY": de_bodyHeight() - HEIGHT_HACK,
            "aaSorting": [[ 1, "desc" ]],
            "aoColumns": [
                { "sTitle": "Trash ID", "mData": "trashId", "sWidth": "20%", "bSortable": false },
                { "sTitle": "Created", "mData": "created", "sWidth": "20%" },
                { "sTitle": "Creator", "mData": "creator", "sWidth": "60%", "bSortable": false, "mRender": renderUser }
            ],
            "oSearch": { "sSearch": $('#input-search').val() },
            "fnInitComplete": function(oSettings, json) {
                $('#dataTable_wrapper').addClass('span24');
                $('#toolbar-container').prepend($('#data-toolbar'));
            },
            "fnCreatedRow": function(nRow, aData) {
                $(nRow).click(function(event) { // pretty crap, should be based on a selection model and events
                    selectedRow = aData;
                    $('#data-toolbar .btn.on-selection').removeClass('disabled');
                    $('#dataTable tr.info').removeClass('info');
                    $(nRow).toggleClass('info');
                });
            }
        });
    }

    var busy = false;

    function goBusy() {
        busy = true;
        $('#modal-wait').modal('show');
    }

    function goIdle() {
        busy = false;
        $('#modal-wait').modal('hide');
    }

    var reloadTable = function() {
        $('#data-toolbar .btn.on-selection').addClass('disabled');
        table.fnFilter($('#input-search').val());
        selectedRow = null;
    };

    $('a.on-selection').click(function(e) {
        if (!selectedRow) { e.preventDefault(); e.stopPropagation(); }
    });

    $('#action-restore').click(function() {
        if (busy || !selectedRow) return;
        goBusy();
        $.post('/api/v2/overlord/trashRecords/' + selectedRow.id + '/restore')
            .done(reloadTable)
            .always(goIdle);
        return false;
    });

    $(function() {
        $('div.dataTables_scrollBody').height( de_bodyHeight() - HEIGHT_HACK );
    });

    ug.initSearchField('#input-search', {}, reloadTable);

    initTable();

 });
