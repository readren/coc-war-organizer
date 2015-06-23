'use strict';
/*global app: false */

app.controller('joinResponseCtrl', ['$scope', function($scope) {
	var jrc = this;
	jrc.hasBeenAccepted = $scope.event.requesterIconDto !== undefined;
	jrc.hasBeenRejected = $scope.event.requesterIconDto === undefined;
} ]);
