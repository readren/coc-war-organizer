'use strict';
/*global app: false */
/**
 * @ngdoc directive
 * @name app.directive:reaDropAnchor
 * @author G Gustavo Pollitzer
 * @description 
 * # reaDropAnchor
 */
app.directive('reaDropAnchor',function(){
	var link = function($scope, $anchorElem) {
		$anchorElem.addClass($scope.anchorClass || 'reaDropAnchor');
	};
	
	var controller = function($scope, $element) {
		var dropDownCtrl = this;
		var $anchorElem = $element;
		var subordinates = [];
		/**called by subordinates from link function to register into this drop down master.*/
		dropDownCtrl.addSubordinate = function(subordinate) {
			subordinates.push(subordinate);
		};
		dropDownCtrl.getAnchorElem = function() {
			return $anchorElem;
		};
		var applyToAllSubordinates = function(op) {
			return function($event) {
				angular.forEach(subordinates, function(subordinate){
					subordinate[op]($event);
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
})
.directive('reaDropPopup', ['$animate', '$document', '$timeout', function($animate, $document, $timeout){
	var link = function($scope, $popupElem, attrs , dropDownCtrl) {
		dropDownCtrl.addSubordinate($scope);
		$popupElem.addClass('ng-hide '+ ($scope.popupClass || 'reaDropPopup'));
		var isShown = false;

		var userEventHandler = function($event) {
			if(angular.isFunction($scope.eventHandler)) {
				$scope.eventHandler(isShown, $event, dropDownCtrl.getAnchorElem(), $popupElem);
			} 
		};
		var hide = function($event) {
			isShown = false;
			userEventHandler($event);
			$animate.addClass($popupElem, 'ng-hide', {tempClasses: 'ng-hide-animate'});
		};
		var hideHandler = function(event) {
			if( !(event.target.compareDocumentPosition($popupElem[0]) & (Node.DOCUMENT_POSITION_CONTAINS|Node.DOCUMENT_POSITION_DISCONNECTED))) {
				$scope.$apply(function(){
					hide(angular.element(event));
				});
				$document.off('click', hideHandler);
			}
		};
		// Exposed API
		$scope.hide = function($event) {
			hide($event);
			$document.off('click', hideHandler);
		};
		$scope.show = function($event) {
			$document.off('click', hideHandler);
			isShown = true;
			userEventHandler($event);
			$animate.removeClass($popupElem, 'ng-hide', {tempClasses: 'ng-hide-animate'});
			$timeout(function() {
				$document.on('click', hideHandler);
			});
		};
		$scope.toggle = function($event) {
			if(isShown) { $scope.hide($event); } else { $scope.show($event); }
		};
	};
	return {
		require: '^reaDropAnchor',
		restrict: 'AE',
		scope: {
			show: '=?reaShow', // gives the function that should be called to show this subordinate content. The function should receive the $event object only if it is needed by the 'reaEventHandler' function.
			hide: '=?reaHide', // gives the function that should be called to hide this subordinate content. The function should receive the $event object only if it is needed by the 'reaEventHandler' function.
			toggle: '=?reaToggle', // gives the function that should be called to toggle this subordinate content. The function should receive the $event object only if it is needed by the 'reaEventHandler' function.
			popupClass: '@?reaPopupClass', // if present, this class, instead of the default (reaDropDownPopup), will be applied to the popuped element. 
			eventHandler: '=?reaEventHandler' // A optional function that will be called every time this popup is showed or hided. It allows to customize the behavior of this directive, for example, to change the popup position. The function receives 3 parameters: (1) a boolean which tells if the popup was showed (true) or hided; (2) the JQuery/JqLite event object (only if the reaShow, reaHide, and reaToggle are called with the $event as parameter), (3) the JQuery/JqLite element of the anchor element (the one with the 'reaDropDown' directive); (4) the JQuery/JqLite element of the popup element (the one where this directive is applied).    
		},
		link: link
	};
}]);
