'use strict';

app.factory('accountService', ['$http', '$q', '$auth', function($http, $q, $auth) {
	
	/** cached accounts */
	var accounts = null;
	/** token with which the cached accounts were retrieved*/
	var ownerToken = null;
	/** knower of the current account */
	var currentAccount = null;

	/** gives the CoC accounts of the current user */
	var getAccountsPromise = function() {
		var deferred = $q.defer();
		
		if( angular.isArray(accounts) && ownerToken === $auth.getToken()) { //if the accounts were already retrieved, give them from the cache
			deferred.resolve(accounts);
		} else { // else, retrieve them from the server
			$http.get('/accounts').then(
				function(value) {
					accounts = value.data;
					ownerToken = $auth.getToken();
					if(accounts.length > 0)
						currentAccount = accounts[0];
					else
						currentAccount = null;
					deferred.resolve(accounts);
				}, function(reason) {
					deferred.reject('Retrieval of accounts from server has failed. Reason: ' + reason.data);
				});
		}
		return deferred.promise;
	};
	
	
	/** adds a new account for current user */
	var addNewAccountPromise = function(newAccountProyect) {
		var deferred = $q.defer();
		$http.post('/addAccount', newAccountProyect).then(
			function(value) {
				var newAccount = value.data;
				if( angular.isArray(accounts))
					accounts = accounts.concat([newAccount]); // note that account reference is changed to facilitate change detection.
				else {
					accounts = [newAccount];
					currentAccount = newAccount;
				}
				deferred.resolve(accounts);
			},
			function(reason) {
				deferred.reject('Addition of new account to server has failed. Reason: ' + reason.data);
			});
		return deferred.promise;
	};
	
	var getCurrentAccount = function() {
		return ownerToken === $auth.getToken() ? currentAccount : null;
	};
	var setCurrentAccount = function(account) { currentAccount = account;};
	
	return {
		getAccountsPromise : getAccountsPromise,
		addNewAccountPromise: addNewAccountPromise,
		getCurrentAccount: getCurrentAccount,
		setCurrentAccount: setCurrentAccount
	};
}]);