'use strict';
/*global app: false */

app.factory('logSrv', [ '$http', '$auth', '$timeout', '$rootScope', 'accountSrv', function($http, $auth, $timeout, $rootScope, accountSrv) {
	var eventsCache = {};
	var lastQueryToken = 0;

	/**updates the cache if a different token is detected
	 * @return true if a different token was detected*/
	var updateCache = function() {
		var currentToken = $auth.getToken();
		if( currentToken !== lastQueryToken) {
			lastQueryToken = currentToken;
			eventsCache = {};
		}
	};
	
	return {
		getLastEventId: function() {
			updateCache();
			var events = eventsCache[accountSrv.getCurrentAccount().tag] || [];
			return events.length === 0 ? null : events[0].id;
		},
	
		getEvents: function() {
			var srv = this;
			return accountSrv.getCurrentAccountPromise().then(
				function(currentAccount) {
					if(currentAccount) {
						return $http.post('/log/getEventsAfter', {eventId: srv.getLastEventId(), accountTag: currentAccount.tag}).then(
							function(valueE) {
								var newEvents = valueE.data.sort(function(a, b) {
									return b.id - a.id;
								});
								// Update old events state. Note that new events are not updated because their controllers aren't instantiated at this moment. Each of them should be updated by itself, calling the updateMe function, during the controller instantiation.  
								angular.forEach(newEvents, function(newEvent) {
									srv.treat(newEvent);
								});
								// add the new events
								var events = eventsCache[currentAccount.tag] || [];
								events = newEvents.concat(events);
								eventsCache[currentAccount.tag] = events;
								// give all the cached events
								return events;
							},
							function(reason) {
								throw reason.data;
							}
						);
					} else {
						return [];
					}
				}
			);
		},
		
		treat: function(incitingEvent) {
			if( incitingEvent.affectedEvents) {
				var events = eventsCache[accountSrv.getCurrentAccount().tag] || [];
				angular.forEach(incitingEvent.affectedEvents, function(affectedEventId) {
					angular.forEach(events, function(event) {
						if(event.id === affectedEventId && event.updateHandler) {
							event.updateHandler(incitingEvent);
						}
					});
				});
			}
		},
		
		updateMe: function(eventId, updateHandler) {
			var events = eventsCache[accountSrv.getCurrentAccount().tag] || [];
			angular.forEach(events, function(incitingEvent) {
				if (incitingEvent.affectedEvents) {
					angular.forEach(incitingEvent.affectedEvents, function(affectedEventId) {
						if(eventId === affectedEventId) {
							updateHandler(incitingEvent);
						}
					});
				}
			});
		} 
	};
	
} ]);
