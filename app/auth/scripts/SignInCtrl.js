'use strict';

/*global app: false */

/**
 * The sign in controller.
 */
app.controller('SignInCtrl', [ '$scope', '$rootScope', 'userService', '$alert', '$auth', function($scope, $rootScope, userService, $alert, $auth) {

	/**
	 * Submits the login form.
	 */
	$scope.submit = function() {
		$auth.login({
			email : $scope.email,
			password : $scope.password })
		.then(userService.getCurrentUserPromise)
		.then(
			function(value){
				$alert({ content : 'You have successfully signed in', animation : 'fadeZoomFadeDown', type : 'material', duration : 3 });
				$rootScope.user = value;
			}, function(response) {
				console.log(response);
				$alert({ content : response.data.message, animation : 'fadeZoomFadeDown', type : 'material', duration : 3 });
				$rootScope.user = null;
			});
	};

	/**
	 * Authenticate with a social provider.
	 *
	 * @param provider The name of the provider to authenticate.
	 */
	$scope.authenticate = function(provider) {
		$auth.authenticate(provider)
		.then(
			function() {
				return userService.getCurrentUserPromise();
			})
		.then(
			function(value) {
				$alert({ content : 'You have successfully signed in', animation : 'fadeZoomFadeDown', type : 'material', duration : 3 });
				$rootScope.user = value;
			},
			function(response) {
				$alert({ content : response.data.message, animation : 'fadeZoomFadeDown', type : 'material', duration : 3 });
				$rootScope.user = null;
			});
		};
} ]);
