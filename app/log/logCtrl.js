'use strict';
/*global app: false */


app.controller('logCtrl', ['logSrv', 'alertSrv', function(logSrv, alertSrv) {
	var ctrl = this;
	
	ctrl.events = [];
	ctrl.refresh = function() {
		logSrv.getEvents().then(
			function(value) {
				ctrl.events = value;
			},
			function(reason) {
				alertSrv.danger('Unable to get log events because: ' + reason);
			}
		);
	};
	ctrl.refresh();
	
}]);
	