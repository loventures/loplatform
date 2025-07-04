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

function startTheEngine() {
    var canvas = document.getElementById('d-e');
    var scale = 0.7;

    paper.setup(canvas);
    var width = paper.view.size.width;
    paper.view.center = [ -510 + 150 + width / 2 / scale, 145 ];
    paper.view.zoom = scale;

    var rects = [];
    var positionRectangles = function(rot) {
        for (var i = 0; i < rects.length; ++ i) {
            rects[i].remove();
        }
        rects = [];
        for (var i = 0; i < 4; ++ i) {
            var r = new paper.Path.Rectangle({
                topLeft: [ 0, 103 - 25 - 28 ],
                bottomRight: [ 28, 103 - 25 ],
                fillColor: '#3e6cd7'
            });
            r.pivot = new paper.Point(14, 150);
            r.rotate(rot + 45 * i);
            rects.push(r);
        }

        if (rot < 22.5) {
            var dx = -rot * 1.25;
            var r = new paper.Path.Rectangle({
                topLeft: [ Math.max(0, 0 + dx), 197 + 25 ],
                bottomRight: [ Math.max(0, 28 + dx), 197 + 25 + 28 ],
                fillColor: '#3e6cd7'
            });
        } else {
            var dx = (-45 + rot) * 1.25;
            var r = new paper.Path.Rectangle({
                topLeft: [ Math.max(0, 0 + dx), 103 - 25 - 28 ],
                bottomRight: [ Math.max(0, 28 + dx), 103 - 25 ],
                fillColor: '#3e6cd7'
            });
        }
        rects.push(r);
    };

    positionRectangles(0);

    new paper.Path.Rectangle({
        topLeft: [ 0, 103 ],
        bottomRight: [ 14, 197 ],
        fillColor: 'white'
    });

    new paper.Path.Arc({
        from: [ 14 - 1, 150 - 47 ],
        through: [ 14 + 47, 150 ],
        to: [ 14 - 1, 150 + 47 ],
        fillColor: 'white'
    });

    paper.view.draw();

    var rectRotation = 0, index = -100, position = -150;

    paper.view.onFrame = function(event) { // .time, .delta
        index = index + 1;
        if (index < 0) {
            return;
        }
        var velocity = Math.max(0, Math.min(720, index)) / 1440;
        var max = width / scale - 110 / scale - 490;
        position = Math.min(position + velocity * 1.25, max);
        paper.view.center = [ -510 - position + width / 2 / scale, 145 ];

        var rotation = (rectRotation + velocity) % 45;
        rectRotation = ((rotation > rectRotation) || (position < max)) ? rotation : 45;
        positionRectangles(((rectRotation < 0) || (rectRotation >= 45)) ? 0 : rectRotation);
        if (rectRotation == 45) index = -1000000; // done
    };

    // 344 x 200
    paper.project.importSVG('/static/static/images/lop-logo2.svg', {
        onLoad: function(de) {
            de.scale(1 / scale, .95 / scale);
            de.translate(-255, 117);
        }
    });
};
