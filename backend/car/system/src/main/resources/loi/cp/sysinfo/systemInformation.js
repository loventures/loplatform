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

var sysInfoApp = angular.module('sysInfoApp', []);
sysInfoApp.controller('sysInfoCtrl', function ($scope) {
        // the last received msg
        $scope.msg = {processors: 0, freeMem: 0, maxMem: 0, totalMem: 0, time:0};

        // handles the callback from the received event
        var handleMem = function (msg) {
            $scope.$apply(function () {
                $scope.msg = JSON.parse(msg.data)
            });
        }

        var handleHost = function (msg) {
                    $scope.$apply(function () {
                        var data = JSON.parse(msg.data)
                        $scope.actorSystem = data.actorSystemAddress
                    });
        }

        var handleGC = function (msg) {
                            $scope.$apply(function () {
                                var data = JSON.parse(msg.data)
                                $scope.lastGC = data
                            });
        }

        var handleCluster = function (msg) {
            $scope.$apply(function () {
                var data = JSON.parse(msg.data)
                $scope.clusterState = data.clusterState
            });
        }

        var source = new EventSource('/event/sys/info');
        source.addEventListener('/sys/info/mem', handleMem, false);
        source.addEventListener('/sys/info/host', handleHost, false);
        source.addEventListener('/sys/info/cluster', handleCluster, false);
        source.addEventListener('/sys/info/gc', handleGC, false);
});
