'use strict';

/**
 * @ngdoc directive
 * @name uiApp.directive:reaDragZone
 * @description
 * # reaDragZone
 */
angular.module('uiApp')
	.directive('reaDragZone', function() {
		return {
			template: '<div></div>',
			restrict: 'AE',
			link: function postLink(scope, element, attrs) {
				element.text('this is the reaDragZone directive');
			}
		};
	});


/**
 * @ngdoc directive
 * @name uiApp.directive:reaDragDock
 * @description
 * # reaDragDock
 */
angular.module('uiApp')
	.directive('reaDragDock', function() {
		return {
			template: '<div></div>',
			restrict: 'AE',
			link: function postLink(scope, element, attrs) {
				element.text('this is the reaDragDock directive');
			}
		};
	});

/**
 * @ngdoc directive
 * @name uiApp.directive:reaDragShip
 * @description
 * # reaDragShip
 */
angular.module('uiApp')
	.directive('reaDragShip', function() {
		return {
			template: '<div></div>',
			restrict: 'AE',
			link: function postLink(scope, element, attrs) {
				
				element.text('this is the reaDragShip directive');
				
			}
		};
	});