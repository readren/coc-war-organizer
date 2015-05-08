/**
 * 
 */
'use strict';

app.controller('accountSettingsCtrl', ['accountService', '$alert', function(accountService, $alert) {
	var asc = this;
	
	// load account selector options
	asc.accounts = [];
	accountService.getAccountsPromise().then(
			function(accounts) {
				asc.accounts = accounts;
				$alert({type: 'success', title:'Success', content: 'The accounts have been retrieved successfully', duration : 3});
			}, function(reason){
				$alert({ type : 'danger', title:'Warning', content: reason, animation : 'fadeZoomFadeDown', duration : 0});
			}, function(notification) {
				$alert({ type: 'info', title:'Info', content: notification, duration : 3});
			});

	// current account switching
	asc.getCurrentAccount = accountService.getCurrentAccount;
//	asc.setCurrentAccount = accountService.setCurrentAccount;
	asc.setCurrentAccount = function(account) {
		asc.currentAccountSetterInterceptor(accountService.setCurrentAccount, account);
	};


	// account adding 
	asc.newAccountProject = {};
	asc.addAccount = function() {
		accountService.addNewAccountPromise(asc.newAccountProject).then(
			function(accounts) {
				asc.accounts = accounts;
				asc.newAccountProject = {};
				$alert({type: 'success', title:'Success', content: 'The new account has been added successfully', duration : 3});
			}, function(reason) {
				$alert({ type : 'danger', title:'Warning', content: reason, animation : 'fadeZoomFadeDown', duration : 0});
			}, function(notification) {
				$alert({ type: 'info', title:'Info', content: notification, duration : 3});
			});
	};
}]);


