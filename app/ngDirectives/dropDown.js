
'use strict';


app.directive('dropDown',['$document', '$timeout', function($document, $timeout){
	var link = function($scope, element, attrs) {
		var $element = angular.element(element);
		$element.toggleClass('dropdown', true);
	};
	
	var controller = function($scope) {
		var ddc = this;
		ddc.contentScope = null;

		var close = function() {
			$scope.$apply(function() {
				ddc.contentScope.isOpen = false;
			});
		};
		var toggle = function() {
			if(ddc.contentScope.isOpen) {
				ddc.contentScope.isOpen = false;
				$document.off('click', close); // TODO is this line necesary?
			} else {
				ddc.contentScope.isOpen = true;
				$timeout(function() {
					$document.one('click', close);
				});
			}
		};
		$scope.toggler = toggle;
	};
	
	return {
		restrcit: 'A',
		scope: {toggler : '=dropDown'},
		link: link,
		controller: controller
	};
}])
.directive('dropDownContent', [function(){
	var link = function($scope, element, attrs, ddc) {
		$scope.isOpen = false;
		ddc.contentScope = $scope;
	};
	return {
		require: '^dropDown',
		restrict: 'E',
		transclude: true,
		scope: {},
		template: '<div class="dropdown-menu" ng-show="isOpen" ng-transclude></div>',
		link: link
	};
}]);
