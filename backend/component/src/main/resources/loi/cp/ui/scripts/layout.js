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

/* The amount of height that a full-page screen should take up in footerless
 * mode. TODO: Kill this, replace it with a riff on loMaximize.js instead, to
 * eliminate the height hacks we have everywhere.  */
function de_bodyHeight() {
    // $('#app .main').offset().top ?
    var main = $('#app > div.main'), paddingY = main.length ? Math.ceil(main.innerHeight() - main.height()) : 0;
    return $(window).height() - ($('header').height() || 0) - paddingY;
}

/* The amount of padding at the bottom of a footerless page. */
function de_paddingBottom() {
    var main = $('#app > div.main'); // noheader mode has no main and no padding
    return main.length ? Math.ceil(main.css('padding-bottom').replace("px","")) : 0;
}

$(function(){
    $('#logout-button').click(function() {
        doLogout()
            .done(function(u) {
                document.location.href = u || '/';
            });
        return false;
    });
    $('#exit-button').click(function() {
        doExit()
            .done(function(u) {
                document.location.href = u || '/';
            });
        return false;
    });
});

$.ajaxSetup({ headers: { 'X-CSRF': 'true' } });
