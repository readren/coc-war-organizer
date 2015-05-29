'use strict';
/*global app: false */

app.controller('clanMembershipCtrl', ['accountSrv', 'membershipSrv', 'alertSrv', '$rootScope', function(accountSrv, membershipSrv, alertSrv, $rootScope) {
	//states: accountLess, initializing, initHasFailed, alone, waitingAcceptance, rejected, joined
	var cmc = this;
	
	var currentAccount = null;
	$rootScope.$watch(function(){
		return accountSrv.getCurrentAccount();
	}, function(newAccount) {
		currentAccount = newAccount;
		cmc.initialize();
	});
	
	var treatMembershipStatus = function(response) {
		if (response.rejectionMsg) {
			cmc.rejectionMsg = response.rejectionMsg;
			cmc.state = 'rejected';
		} else if( response.memberDto) {
			cmc.currentOrganization = response.organization;
			cmc.memberDto = response.memberDto;
			cmc.state = 'joined';
		} else if( response.organization ) {
			cmc.pretendedOrganization = response.organization;
			alertSrv.info('Your join request was sent. Please wait a leader accepts you.');
			cmc.state = 'waitingAcceptance';
		} else {
			cmc.state = 'alone';
		}
	};

	cmc.initialize = function() {
		cmc.currentOrganization = null;
		cmc.rejectionMsg = null;
		if(currentAccount) {
			membershipSrv.getMembershipStatusOfPromise(currentAccount).then(
				treatMembershipStatus,
				function(reason) {
					alertSrv.danger('Unable to initialize the clan membership because: ' + reason);
					cmc.state = 'initHasFailed';
				});
			cmc.state = 'initializing';
		} else {
			cmc.state = 'accountLess';
		}
	};
	cmc.initialize();
	
	cmc.searchOrganizationsCmd = {};
	cmc.searchOrganizations = function() {
		cmc.searchResult = [];
		cmc.aSearchIsInProgress = true;
		cmc.showSearchResult();
		membershipSrv.searchOrganizationsPromise(cmc.searchOrganizationsCmd).then(
			function(searchResult){
				cmc.aSearchIsInProgress = false;
				cmc.searchResult = searchResult;
			},
			function(reason){
				cmc.aSearchIsInProgress = false;
				alertSrv.warning('Unable to search for organizations because: ' + reason);
			});
	};
	
	cmc.sendJoinRequest = function(organization) {
		cmc.hideSearchResult();
		cmc.hideOrganizationJoiningForm();
		membershipSrv.sendJoinRequestPromise(accountSrv.getCurrentAccount(), organization).then(
			treatMembershipStatus,
			function(reason){
				alertSrv.danger('Unable to send the join request because: ' + reason);
			});
	};
	
	cmc.checkJoinResponse = function() {
		membershipSrv.getMembershipStatusOfPromise(accountSrv.getCurrentAccount()).then(
			treatMembershipStatus,
			function(reason){
				alertSrv.danger('Unable to check the join request\'s response because: ' + reason);
			});
	};
	
	cmc.cancelJoinRequest = function() {
		membershipSrv.cancelJoinRequestPromise(accountSrv.getCurrentAccount()).then(
			treatMembershipStatus,
			function(reason) {
				alertSrv.danger('Unable to cancel join request because: ' + reason);
			});
	};
	
	cmc.leaveOrganization = function() {
		membershipSrv.leaveOrganizationPromise(accountSrv.getCurrentAccount()).then(
			treatMembershipStatus,
			function(reason) {
				alertSrv.danger('Unable to leave the organization because: '+ reason);
			}
		);
	};
	
	cmc.createOrganizationCmd = {};
	cmc.createOrganization = function() {
		membershipSrv.createOrganizationPromise(accountSrv.getCurrentAccount(), cmc.createOrganizationCmd).then(
			function(response) {
				cmc.hideOrganizationCreationForm();
				cmc.currentOrganization = response.organization;
				cmc.memberDto = response.memberDto;
				alertSrv.success('The new organization has been created successfully');
				cmc.state = 'joined';
			},
			function(reason) {
				alertSrv.warning('Unable to create an organization because: ' + reason);
			});
	};
	
}]);