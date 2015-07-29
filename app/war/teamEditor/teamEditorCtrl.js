'use strict';
/*global app: false */

/**
 * @ngdoc function
 * @name uiApp.controller: teamEditorCtrl
 * @description
 * # teamEditorCtrl
 * Controller of the uiApp
 */
app.controller('teamEditorCtrl', function (centralSrv, logSrv, alertSrv, utilSrv) {
	var ctrl = this;
	ctrl.freeMembers = []; // Seq[IconDto]
	ctrl.slots = []; // Seq[AddParticipantEvent | EmptySlot]
	
	logSrv.getMembers().then(
		function(members) {
			var currentMembers = members;
			centralSrv.getWarState()
			.then(
				function(warState){
					ctrl.slots = warState.slots;
					// free members are those members who are not participating
					ctrl.freeMembers = utilSrv.collect(currentMembers, function(member) {
						return utilSrv.find(ctrl.slots, function(slot) {
							return slot.iconTag == member.tag;
						}) === null ? member : undefined;
					});
				},
				function(reason){
					alertSrv.danger('The war state could not be fetched because: ' + reason);
				});
		}, function(reason) {
			alertSrv.danger('The current members set could not be fetched because: ' + reason);
		}
	);
	
	
  });

