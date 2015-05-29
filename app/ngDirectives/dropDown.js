'use strict';
/*global app: false */

app.directive('reaDropDown',[function(){
	var link = function($scope, $element) {
		$element.addClass($scope.anchorClass || 'dropdown');
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
		scope: {
			show: '=?reaShow', // gives the function that should be called to show all the subordinate contents
			hide: '=?reaHide', // gives the function that should be called to hide all the subordinate contents
			toggle: '=?reaToggle', // gives the function that should be called to toggle all the subordinate contents
			anchorClass: '@?reaAnchorClass'
		},
		link: link,
		controller: controller
	};
}])
.directive('reaDropDownContent', ['$animate', '$document', '$timeout', function($animate, $document, $timeout){
	var link = function($scope, $element, attrs , dropDownCtrl) {
		dropDownCtrl.addSubordinate($scope);
		$element.addClass('ng-hide '+ ($scope.dropClass || 'dropdown-menu'));
		var isShown = false;
		var hide = function() {
			isShown = false;
			$animate.addClass($element, 'ng-hide', {tempClasses: 'ng-hide-animate'});
		};
		var hideHandler = function(event) {
			if( !(event.target.compareDocumentPosition($element[0]) & Node.DOCUMENT_POSITION_CONTAINS)) {
				$scope.$apply(hide);
				$document.off('click', hideHandler);
			}
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
				$document.on('click', hideHandler);
			});
		};
		$scope.toggle = function() {
			if(isShown) { $scope.hide(); } else { $scope.show(); }
		};
	};
	return {
		require: '^reaDropDown',
		restrict: 'AE',
		scope: {
			show: '=?reaShow', // gives the function that should be called to show this subordinate content
			hide: '=?reaHide', // gives the function that should be called to hide this subordinate content
			toggle: '=?reaToggle', // gives the function that should be called to toggle this subordinate content
			dropClass: '@?reaDropClass'
		},
		link: link
	};
}]);
