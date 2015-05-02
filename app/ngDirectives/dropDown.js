'use strict';

app.directive('reaDropDown',[function(){
	var link = function($scope, $element, attrs) {
		$element.addClass('dropdown');
	};
	
	var controller = function($scope) {
		var dropDownCtrl = this;
		var subordinates = [];
		/**called by subordinates from link function to register into this drop down master.*/
		dropDownCtrl.addSubordinate = function(subordinate) {
			subordinates.push(subordinate);
		};
		var applyToAllSubordinates = function(op) {
			return function() {
				angular.forEach(subordinates, function(subordinate){
					subordinate[op]();
				});
			};
		};
		// Exposed API
		$scope.show = applyToAllSubordinates('show');
		$scope.hide = applyToAllSubordinates('hide');
		$scope.toggle = applyToAllSubordinates('toggle');
	};
	
	return {
		restrcit: 'EA',
		scope: {show: '=?reaShow', hide: '=?reaHide', toggle: '=?reaToggle'},
		link: link,
		controller: controller
	};
}])
.directive('reaDropDownContent', ['$animate', '$document', '$timeout', function($animate, $document, $timeout){
	var link = function($scope, $element, attrs , dropDownCtrl) {
		dropDownCtrl.addSubordinate($scope);
		$element.addClass('ng-hide dropdown-menu');
		var isShown = false;
		var hide = function() {
			isShown = false;
			$animate.addClass($element, 'ng-hide', {tempClasses: 'ng-hide-animate'});
		};
		var hideHandler = function() {
			$scope.$apply(hide);
		};

		// Exposed API
		$scope.hide = function() {
			hide();
			$document.off('click', hideHandler);
		};
		$scope.show = function() {
			isShown = true;
			$animate.removeClass($element, 'ng-hide', {tempClasses: 'ng-hide-animate'});
			$timeout(function() {
				$document.one('click', hideHandler);
			});
		};
		$scope.toggle = function() {
			isShown ? $scope.hide() : $scope.show();
		};
	};
	return {
		require: '^reaDropDown',
		restrict: 'AE',
		scope: {show: '=?reaShow', hide: '=?reaHide', toggle: '=?reaToggle'},
		link: link
	};
}]);
