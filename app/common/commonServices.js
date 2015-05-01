'use strict';

app.factory('accountService', ['$http', '$q', function($http, $q) {
	
	var accounts = null;
	var currentAccount = null;

	var getAccountsPromise = function() {
		var deferred = $q.defer();
		
		if( angular.isArray(accounts)) { //if the accounts were already retrieved, give them
			deferred.resolve(accounts);
		} else { // else, retrieve them from the server
			$http.get('/accounts').then(
				function(value) {
					accounts = value.data;
					if(accounts.length>0) currentAccount = accounts[0];
					deferred.resolve(accounts);
				}, function(reason) {
					deferred.reject('Retrieval of accounts from server has failed. Reason: ' + reason.data);
				});
		}
		return deferred.promise;
	};
	
	
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
	
	return {
		getAccountsPromise : getAccountsPromise,
		addNewAccountPromise: addNewAccountPromise,
		getCurrentAccount: function() { return currentAccount; },
		setCurrentAccount: function(account) { currentAccount = account;}
	};
}]);