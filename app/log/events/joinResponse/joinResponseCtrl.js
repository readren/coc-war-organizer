'use strict';
/*global app: false */

app.controller('joinResponseCtrl', ['$scope', function($scope) {
	var jrc = this;
	jrc.hasBeenAccepted = $scope.event.requesterMemberName !== undefined;
	jrc.hasBeenRejected = $scope.event.requesterMemberName === undefined;
} ]);
