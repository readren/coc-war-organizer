'use strict';
/*global app: false */

app.factory('membershipSrv', ['utilsSrv', '$http', '$q', '$auth',function(utilsSrv, $http, $q, $auth){
	var currentOrganizationCache = {};
	var lastQueryToken = 0;
	
	/**updates the cache if a different token is detected
	 * @return true if a different token was detected*/
	var updateCache = function() {
		var currentToken = $auth.getToken();
		if( currentToken !== lastQueryToken) {
			lastQueryToken = currentToken;
			currentOrganizationCache = {};
			return true;
		} else {
			return false;
		}
	};
	
	/**gives the same response than received promise but, if the account is member of an organization, the membershipStatus is stored in the account->membershipStatus cache */
	var treatMembershipStatus = function(accountTag, deferred, promise) {
		promise.then(
			function(response) {
				if(response.data.organization && response.data.memberDto) {
					currentOrganizationCache[accountTag] = response.data;
				}
				deferred.resolve(response.data);
			},
			function(reason) {
				deferred.reject(reason.data);
			});
		return deferred.promise;
	};
	
	return {
		getMembershipStatusOfPromise: function(account) {
			var deferred = $q.defer();
			updateCache();
			return treatMembershipStatus(account.tag, deferred, $http.get('/membership/getMembershipStatusOf/' + account.tag));
		},
		
		/**Search for all the organizations that satisfy the received criteria */
		searchOrganizationsPromise: function(searchOrganizationsCmd) {
			return utilsSrv.mapHttpData($http.post('/membership/searchOrganizations', searchOrganizationsCmd));
		},
		
		getCurrentOrganizationPromise: function(account) {
			var deferred = $q.defer();
			if( !updateCache() && currentOrganizationCache[account.tag]) {
				deferred.resolve(currentOrganizationCache[account.tag]);
				return deferred.promise;
			} else {
				return treatMembershipStatus(account.tag, deferred, $http.get('/membership/getMembershipStatusOf/' + account.tag));
			}
		},
		
		sendJoinRequestPromise: function(account, organization) {
			var deferred = $q.defer();
			updateCache();
			return treatMembershipStatus(account.tag, deferred, $http.post('/membership/sendJoinRequest', {accountTag: account.tag, organizationId: organization.id}));
		},

		cancelJoinRequestPromise: function(account) {
			var deferred = $q.defer();
			updateCache();
			return treatMembershipStatus(account.tag, deferred, $http.post('/membership/cancelJoinRequest', account.tag));
		},

		leaveOrganizationPromise: function(account) {
			var deferred = $q.defer();
			updateCache();
			return treatMembershipStatus(account.tag, deferred, $http.post('/membership/leaveOrganization', account.tag));
		},
		
		createOrganizationPromise: function(account, createOrganizationCmd) {
			createOrganizationCmd.accountTag = account.tag;
			createOrganizationCmd.accountName = account.name;
			return utilsSrv.mapHttpData($http.post('/membership/createOrganization', createOrganizationCmd));
		}
	};
}]);