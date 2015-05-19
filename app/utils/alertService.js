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
		}
	};
}]);