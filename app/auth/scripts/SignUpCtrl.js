'use strict';

/*global app: false */

/**
 * The sign up controller.
 */
app.controller('SignUpCtrl', [ '$scope', '$rootScope', 'userService', '$alert', '$auth', function($scope, $rootScope, userService, $alert, $auth) {

	/**
	 * The submit method.
	 */
	$scope.submit = function() {
		$auth.signup({ firstName : $scope.firstName, lastName : $scope.lastName, email : $scope.email, password : $scope.password })
		.then(userService.getCurrentUserPromise)
		.then(
			function(value) {
				$alert({ content : 'You have successfully signed up', animation : 'fadeZoomFadeDown', type : 'material', duration : 3 });
				$rootScope.user = value;
			}, function(response) {
				$alert({ content : response.data.message, animation : 'fadeZoomFadeDown', type : 'material', duration : 3 });
				$rootScope.user = null;
			});
	};
} ]);
