'use strict';
/*global app: false */

app.factory('accountSrv', ['$http', '$q', '$auth', function($http, $q, $auth) {
	
	/** token with which the cached accounts were retrieved*/
	var ownerToken = null;
	/** cached accounts */
	var accountsCache = null;
	/** knower of the current account */
	var currentAccount = null;
	/** the accounts promise */
	var accountsPromise = null;
	/** updates this service when the token changes*/
	var update = function() {
		if( ownerToken !== $auth.getToken()) {
			accountsCache = null;
			currentAccount = null;
			accountsPromise = null;
		}
	};
	
	/** gives the CoC accounts of the current user */
	var getAccountsPromise = function() {
		update();
		if(accountsCache) {
			accountsPromise = $q.when(accountsCache);
		} else if(!accountsPromise) {
			accountsPromise = $http.get('/account/getAll').then(
				function(value) {
					accountsCache = value.data;
					ownerToken = $auth.getToken();
					if(value.data.length > 0) {
						currentAccount = value.data[0];
					}
					return value.data;
				}, function(reason) {
					throw reason.data;
				});
		}
		return accountsPromise;
	};
	
	
	/** adds a new account for current user */
	var addNewAccountPromise = function(newAccountProyect) {
		return $http.post('/account/create', newAccountProyect).then(
			function(value) {
				var newAccount = value.data;
				accountsCache = accountsCache.concat([newAccount]); // note that account reference is changed to facilitate change detection.
				return accountsCache;
			},
			function(reason) {
				throw reason.data;
			});
	};
	
	var getCurrentAccount = function() {
		return ownerToken === $auth.getToken() ? currentAccount : null;
	};
	var setCurrentAccount = function(account) { currentAccount = account;};
	
	var getCurrentAccountPromise = function() {
		update();
		if(currentAccount || (accountsCache && accountsCache.length===0)) {
			return $q.when(currentAccount);
		} else {
			return getAccountsPromise().then(
				function(accounts) {
					return accounts[0];
				},
				function(reason) {
					throw reason;
				}
			);
		}
	};
	
	return {
		getAccountsPromise : getAccountsPromise,
		addNewAccountPromise: addNewAccountPromise,
		getCurrentAccount: getCurrentAccount,
		setCurrentAccount: setCurrentAccount,
		getCurrentAccountPromise: getCurrentAccountPromise
	};
}]);