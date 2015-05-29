'use strict';
/*global app: false */

app.factory('alertSrv', ['$alert', function($alert){
	var animation =  'fadeZoomFadeDown';
	var duration = function(message) { return 3 + message.length / 10; };
	return {
		success: function(message) {
			$alert({type: 'success', title:'Success', content: message, animation : animation, duration : duration(message)});
		},
		info: function(message) {
			$alert({ type : 'info', title:'Info', content: message, animation : animation, duration : duration(message)});
		},
		warning: function(message) {
			$alert({ type : 'warning', title:'Warning', content: message, animation : animation, duration : 0});
		},
		danger: function(message) {
			$alert({ type : 'danger', title:'Error', content: message, animation : animation, duration : 0});
		},
		/**Gives a function that receives a message and shows it using one of the other operations of this service.
		 * @param type a string that determines which operation to use. Valid values are: 'success', 'info', 'warning', 'danger'.
		 * @param prefix a string that would be prepended to the message received by the given function
		 * @param sufix a string that would be appended to the message received by the given function*/
		build: function(type, prefix, sufix) {
			var srv = this;
			return function(main) {
				srv[type]((prefix?prefix:'') + (main?main:'') + (sufix?sufix:''));
			};
		}
	};
}]);