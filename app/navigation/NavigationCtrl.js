'use strict';

/*global app: false */

/**
 * The navigation controller.
 */
app.controller('NavigationCtrl', ['$rootScope', '$scope', '$auth', 'userService', function($rootScope, $scope, $auth, userService) {

	var autoLogin = true;
	if(autoLogin) {
		/** logs in, initializes the user object, and switches to account settings page */
		$auth.login({ email: 'borrame@ya', password: 'ya' })
		.then(userService.getCurrentUserPromise)
		.then(function(value){
			$rootScope.user = value;
		})
		['catch'](function(response) {
			console.log(response);
		});
	}
	
  /**
   * Indicates if the user is authenticated or not.
   *
   * @returns {boolean} True if the user is authenticated, false otherwise.
   */
	$scope.isAuthenticated = function() {
		return $auth.isAuthenticated();
	};
	$scope.isIdentifying = function() {
		return $auth.isAuthenticated() && $rootScope.user == null;
	};
	$scope.isIdentifyed = function() {
		return $auth.isAuthenticated() && $rootScope.user != null;
	};


}]);
