/**
 * 
 */
'use strict';
/*global app: false */

app.controller('accountChooserCtrl', ['accountService', 'alertSrv', function(accountService, alertSrv) {
	var acc = this;
	
	// load account selector options
	acc.accounts = [];
	accountService.getAccountsPromise().then(
			function(accounts) {
				acc.accounts = accounts;
				alertSrv.success('The accounts have been retrieved successfully');
			}, function(reason){
				alertSrv.warning(reason);
			}, function(notification) {
				alertSrv.info(notification);
			});

	// current account switching
	acc.getCurrentAccount = accountService.getCurrentAccount;
//	acc.setCurrentAccount = accountService.setCurrentAccount;
	acc.setCurrentAccount = function(account) {
		acc.currentAccountSetterInterceptor(accountService.setCurrentAccount, account);
	};


	// account adding 
	acc.newAccountProject = {};
	acc.addAccount = function() {
		accountService.addNewAccountPromise(acc.newAccountProject).then(
			function(accounts) {
				acc.accounts = accounts;
				acc.newAccountProject = {};
				alertSrv.success('The new account has been added successfully');
			}, function(reason) {
				alertSrv.warning(reason);
			}, function(notification) {
				alertSrv.info(notification);
			});
	};
}]);


