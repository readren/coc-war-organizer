/**
 * 
 */
'use strict';
/*global app: false */

app.controller('accountChooserCtrl', ['accountSrv', 'alertSrv', function(accountSrv, alertSrv) {
	var acc = this;
	
	// load account selector options
	acc.accounts = [];
	accountSrv.getAccountsPromise().then(
			function(accounts) {
				acc.accounts = accounts;
			},
			alertSrv.build('danger', 'Unable to retrieve accounts because: '),
			alertSrv.build('info'));

	// current account switching
	acc.getCurrentAccount = accountSrv.getCurrentAccount;
	acc.setCurrentAccount = function(account) {
		acc.currentAccountSetterInterceptor(accountSrv.setCurrentAccount, account);
		acc.toggleAccountSelector();
	};


	// account adding 
	acc.newAccountProject = {};
	acc.addAccount = function() {
		accountSrv.addNewAccountPromise(acc.newAccountProject).then(
			function(accounts) {
				if( acc.accounts.length===0 ) {
					acc.currentAccountSetterInterceptor(accountSrv.setCurrentAccount, accounts[0]);
				}
				acc.accounts = accounts;
				acc.newAccountProject = {};
				acc.toggleAccountAdder();
				alertSrv.success('The new account has been added successfully');
			},
			alertSrv.build('danger', 'Unable to add the account because: '),
			alertSrv.build('info'));
	};
}]);


