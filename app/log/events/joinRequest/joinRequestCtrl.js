'use strict';
/*global app: false */

app.controller('joinRequestCtrl', [ 'joinRequestSrv', '$scope', 'alertSrv', 'logSrv', function(joinRequestSrv, $scope, alertSrv, logSrv) {
	var jrc = this;
	jrc.requesterAccountName = $scope.event.accountName;
	jrc.isWaiting = true;
	jrc.hasBeenAccepted = false;
	jrc.hasBeenRejected = false;
	jrc.hasBeenCanceled = false;

	jrc.accept = function() {
		joinRequestSrv.accept($scope.event.id).then(
			null,
			alertSrv.build('danger', 'Unable to accept the join request because: '));
	};

	jrc.reject = function() {
		joinRequestSrv.reject($scope.event.id, jrc.rejectionMsg).then(
			null,
			alertSrv.build('danger', 'Unable to reject the join request because: ')
		);
	};

	$scope.event.updateHandler = function(incitingEvent) {
		if(incitingEvent.type === 'joinCancel') {
			jrc.hasBeenCanceled = true;
			jrc.isWaiting = false;
		} else if(incitingEvent.type === 'joinResponse'){
			jrc.responderIconName = incitingEvent.responderIconName;
			jrc.rejectionMsg = incitingEvent.rejectionMsg;
			jrc.hasBeenAccepted = incitingEvent.requesterIconDto !== undefined;
			jrc.hasBeenRejected = incitingEvent.requesterIconDto === undefined;
			jrc.isWaiting = false;
		} else {
			throw 'Invalid inciting event:' + incitingEvent.toString();
		}
			
	};
	
	logSrv.updateMe($scope.event.id, $scope.event.updateHandler);
} ]);


app.factory('joinRequestSrv', [ 'accountSrv', 'utilsSrv', 'logSrv', '$http', function(accountSrv, utilsSrv, logSrv, $http) {
	return {
		accept: function(eventId) {
			return $http.post('log/joinRequest/accept', { // log.events.joinRequest.JoinRespondCmd
				responderAccountTag: accountSrv.getCurrentAccount().tag,
				requestEventId: eventId
			}).then(
				function(value) {
					logSrv.treat(value.data);
				},
				function(reason) {
					throw reason.data;
				}
			);
		},
		
		reject: function(eventId, rejectionMsg) {
			return $http.post('log/joinRequest/reject', { // log.events.joinRequest.JoinRespondCmd
				responderAccountTag: accountSrv.getCurrentAccount().tag,
				requestEventId: eventId,
				rejectionMsg: rejectionMsg
			}).then(
				function(value) {
					logSrv.treat(value.data);
				},
				function(reason) {
					throw reason.data;
				});
		}
	};

} ]);
