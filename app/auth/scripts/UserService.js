'use strict';

/*global app: false */

/**
 * The user factory.
 */
app.factory('userService', ['$http', '$auth', '$q', function($http, $auth, $q) {
	var currentUser = null;
	var currentToken = null;
	
	/**Gives a promise of the current user */
	var getCurrentUserPromise = function () {
		var deferred = $q.defer();
		if( !$auth.isAuthenticated())
			deferred.reject('No user is currently authenticated');
		else if( currentUser!=null && currentToken === $auth.getToken())
			deferred.resolve(currentUser);
		else {
			$http.get('/user').then(
				function(value) {
					currentUser = value.data;
					currentToken = $auth.getToken();
					deferred.resolve(currentUser);
				}, function(reason) {
					deferred.reject('Retrieval of the user from server has failed. Reason: ' + reason.data);
				});
		}
		return deferred.promise;
	};
	
	return { getCurrentUserPromise : getCurrentUserPromise };
		
}]);
