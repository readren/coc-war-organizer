'use strict';
/*global app: false */

app.controller('clanMembershipCtrl', ['accountService', 'membershipSrv', 'alertSrv', '$rootScope', function(accountService, membershipSrv, alertSrv, $rootScope) {
	//states: accountLess, initializing, initHasFailed, alone, waitingAcceptance, rejected, joined
	var cmc = this;
	
	var currentAccount = null;
	$rootScope.$watch(function(){
		return accountService.getCurrentAccount();
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
	cmc.searchOrganizations = function($event){
		cmc.searchResult = [];
		$event.stopPropagation();
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
		membershipSrv.sendJoinRequestPromise(accountService.getCurrentAccount(), organization).then(
			treatMembershipStatus,
			function(reason){
				alertSrv.danger('Unable to send the join request because: ' + reason);
			});
	};
	
	cmc.checkJoinResponse = function() {
		membershipSrv.getMembershipStatusOfPromise(accountService.getCurrentAccount()).then(
			treatMembershipStatus,
			function(reason){
				alertSrv.danger('Unable to check the join request\'s response because: ' + reason);
			});
	};
	
	cmc.cancelJoinRequest = function() {
		membershipSrv.cancelJoinRequestPromise(accountService.getCurrentAccount()).then(
			function() {
				cmc.state = 'alone';
			},
			function(reason) {
				alertSrv.danger('Unable to cancel join request because: ' + reason);
			});
	};
	
	cmc.leaveOrganization = function() {
		membershipSrv.leaveOrganizationPromise(accountService.getCurrentAccount()).then(
			treatMembershipStatus,
			function(reason) {
				alertSrv.danger('Unable to leave the organization because: '+ reason);
			}
		);
	};
	
	cmc.createOrganizationCmd = {};
	cmc.createOrganization = function() {
		membershipSrv.createOrganizationPromise(accountService.getCurrentAccount(), cmc.createOrganizationCmd).then(
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