'use strict';

/*global app: false */

/**
 * The sign out controller.
 */
app.controller('SignOutCtrl', [ '$auth', '$rootScope', '$alert', function($auth, $rootScope, $alert) {
	if ($auth.isAuthenticated()) {
		$rootScope.user = null;
		$auth.logout().then(function() {
			$alert({ content : 'You have been logged out', animation : 'fadeZoomFadeDown', type : 'material', duration : 3 });
		});
	}
}]);
