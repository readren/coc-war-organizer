'use strict';

app.provider('reaVcAnimService', function() {
	var defaultProfileKey = 'change';
	this.setDefaultProfileKey = function(profileKey) {
		defaultProfileKey = profileKey;
	};
	
	this.$get = ['$animate', function($animate) {
		var profiles = {
			leaveEnter: {
				prelude: function($scope, $element, attrs){
					return $animate.animate($element, {}, {}, 'ng-leave', {tempClasses: 'ng-leave-animate'});
				},
				postude:function($scope, $element, attrs){
					return $animate.animate($element, {}, {}, 'ng-enter', {tempClasses: 'ng-enter-animate'});
				}
			},
			hideShow: {
				prelude: function($scope, $element, attrs){
					return $animate.addClass($element, 'ng-hide', {tempClasses: 'ng-hide-animate'});
				},
				postlude:function($scope, $element, attrs){
					return $animate.removeClass($element, 'ng-hide', {tempClasses: 'ng-hide-animate'});
				}
			},
			change: {
				prelude: function($scope, $element, attrs){
					return $animate.addClass($element, 'rea-change', {tempClasses: 'rea-change-animate'});
				},
				postlude:function($scope, $element, attrs){
					return $animate.removeClass($element, 'rea-change', {tempClasses: 'rea-change-animate'});
				}
			}};
		this.addProfile = function(key, profile) {
			profiles = angular.extend(profiles, {key: profile});
		};
			
		return {
			getProfile : function(key) {
				return profiles[key];
			},
			defaultProfile: profiles[defaultProfileKey]
		};
	}];
});

/**Useful when you want to animate the change of a value keeping the old value some time in the animation, despite the new value is already set internally.*/
app.directive('reaVcAnim',['$q', 'reaVcAnimService', function($q, reaVcAnimService){
	var link = function($scope, $element, attrs){
		var isPreludeTime = false;
		var lastFetchedValue = null;
		
		$scope.setterInterceptor = function(valueSetter) { // note that the context of this function might not be the scope of this directive. Usually would be a parent scope. So, 'this' must not be used.
			isPreludeTime = true;
			var valueSetterParams = [].slice.apply(arguments).slice(1, arguments.length);
			var profile = angular.isString($scope.profileKey) ? reaVcAnimService.getProfile($scope.profileKey) : reaVcAnimService.defaultProfile;   
			
			var preludeAnimPromise = profile.prelude($scope, $element, attrs); // the prelude animation is started before the valueSetter in order to support the case when the valueSetter is slow and synchronous
			var valueSetterResult = valueSetter.apply(this, valueSetterParams);  
			var valueChangePromise = $q.when(valueSetterResult);
			valueChangePromise['finally'](function(){
				preludeAnimPromise.then(function(){
					isPreludeTime = false;
					// returns a promise of the postlude animation
					var postludeAnimPromise = profile.postlude($scope, $element, attrs);
					$scope.$root.$digest(); // don't ask me why, but is needed in order to work. At least in angularJs 1.3.0. I discovered that myself debugging: at some point, the animate function wait a digest be performed to continue.
					return postludeAnimPromise;
				})['catch'](function(reason){
					isPreludeTime = false;
					console.log('An animation performed by the "rea-vc-anim" directive has throwed the following error: '+ reason);
				});
			});
			return valueSetterResult;
		};
		
		$scope.getterInterceptor = function(valueGetter) { // note that the context of this function might not be the scope of this directive. Usually would be a parent scope. So, 'this' must not be used.
			if(!isPreludeTime) {
				var valueGetterParams = [].slice.apply(arguments).slice(1, arguments.length);
				lastFetchedValue = valueGetter.apply(this, valueGetterParams);
			}
			return lastFetchedValue;
		};
	};
	return {
		restrict: 'AE',
		scope: {
			setterInterceptor: '=reaVcAnimSetInter', // gives the function inside which the actual value setter operation should be wrapped. The given function (the setterInterceptor) should receive the actual 'valueSetter' function that performs the actual value change. The received 'valueSetter' function will be called simultaneously with the "prelude" animation. The "postlude" animation is started when both, the "prelude" animation and the 'valueSetter' function have finished. All these is programmed in promises, the 'setterInterceptor' functions returns as soon as the wrapped function (the valueSetter function) has finished, giving the same return value the 'valueWrager' has given.
			getterInterceptor: '=reaVcAnimGetInter', // gives the function inside which the actual value getter operation should be wrapped. The given function (the getterInterceptor) should receive the actual  'valueGetter' function that fetches the current value.
			profileKey: '@?reaVcAnimProfileKey'
		},
		link : link
	};
}]);


app.directive('reaChangeAnim',['$animate', '$q', function($animate, $q){
	var link = function($scope, $element, attrs){
		$scope.fire = function(changer) {
			var preChangeAnim = $scope.preChangeAnim();
			var postChangeAnim = $scope.postChangeAnim();
			var performChangeAndPostAnim = function() {
				return (
					$q.when(changer())
					['finally'](function() {
						if(angular.isObject(postChangeAnim))
							$animate.animate($element, postChangeAnim.from, postChangeAnim.to, postChangeAnim.class, postChangeAnim.options);
					}));
			};
			return (
				(angular.isObject(preChangeAnim)
						? $animate.animate($element, preChangeAnim.from, preChangeAnim.to, preChangeAnim.class, preChangeAnim.options)
						: $q.when(null)
				).then(performChangeAndPostAnim)
				['catch'](function(reason) {
					console.log(reason);
				}));
		};
	};
	return {
		restrict: 'AE',
		scope: {
			fire: '=reaFireChangeAnim', // gives the function that should be called to perform the change wrapped by the animations. The given function (the firer) should receive a function (the changer function) which will be called after the 'preChangeAnim' have succeeded, and before the 'postChangeAnim' is started. If the 'changer' function returns a promise, the second animation will start after that promise has finished. Returns a promise with the result of the call to the received 'changer' function, or the reason of the failure of the 'preChangeAnim' if it failed, in which case the 'changer' function is not called.  
			preChangeAnim: '&?reaPreChangeAnim', // the animation that would be performed before the 'changer' function (received as parameter of the 'fireChangeAnim' function) is called.
			postChangeAnim: '&?reaPostChangeAnim' // the animation that would be performed after the 'changer' function (received as parameter of the 'fireChangeAnim' function) has been called. If the 'changer' function returns a promise, this animation will start after that promise has finished, successfully or not.
		},
		link : link
	};
}]);

app.directive('reaAnim',['$animate','$q', function($animate, $q){
	var link = function($scope, $element, attrs) {
		$scope.fire = function() {
			var promise = $q.when(true);
			angular.forEach($scope.frames(), function(frame) {
				promise.then(function() {
					$animate.animate($element, frame.from, frame.to, frame.class, frame.options);
				});
			});
		};
	};
	return {
		restrict: 'AE',
		scope: {
			fire: '=reaAnimFire', // gives the function that should be called to fire the animation
			frames: '&reaAnimFrames' // receives the sequence of frames that will be used for the animation. Each frame contains the parameters to the angular's $animate.animate function.
		},
		link: link
	};
}]);

