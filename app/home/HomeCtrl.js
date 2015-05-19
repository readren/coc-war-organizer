'use strict';

/*global app: false */

/**
 * The home controller.
 */
app.controller('HomeCtrl', ['$animate', function($animate) {
	var hc = this;
	var $element = angular.element('#pepe');
	hc.isVisible = true;
	hc.onClick = function() {
		if(hc.isVisible) {
			hc.isVisible = false;
			$animate.addClass($element, 'ng-hide', {tempClasses: 'ng-hide-animate'});
		} else {
			hc.isVisible = true;
			$animate.removeClass($element, 'ng-hide', {tempClasses: 'ng-hide-animate'});
		}
		
	};
	
}]);
