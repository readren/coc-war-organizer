'use strict';

/**
 * @ngdoc service
 * @name uiApp.centralSrv
 * @description
 * # centralSrv
 * Factory in the uiApp.
 */
angular.module('uiApp').factory('centralSrv', function($http, $q, commonSrv, utilsSrv) {

	var fewerSuweFirst = function(a, b) { return b.suwe - a.suwe; };
	var earlyerInstantFirst = function(a, b) { return a.id - b.id; };

	var EmptySlot = function(position) {
		this.position = position;
	};
	
	var WarState = function() {
		this.events = [];
		this.slots = utilSrv.fill(50, function(i){ return new EmptySlot(i); });
		this.battleHistory = [];
		this.warPhase = 'preparation'; // possible values are: 'preparation', 'battle', and 'postWar'
		this.guesses = [];		
	};
	WarState.prototype.digest(event) {
		this.events.push(event);
		if(event.type === 'StartPreparation') {
			WarState.call(this);
		} else if(event.type === 'AddParticipant') {
			this.slots[event.basePosition] = event;
		} else if(event.type === 'StartBattle') {
			this.warPhase = 'battle'; 
		} else if(event.type === 'AddGuess') {
			this.guesses.push(event);
		} else if(event.type === 'AddDefense' || event.type === 'AddAttack') {
			this.battleHistory.push(event);
			this.battleHistory.sort(fewerSuweFirst);
		} else if(event.type === 'EndWar') {
			this.warPhase = 'postWar';
		} else if(event.type === 'Undo') {
			var undoneEvent = utilsSrv.find(this.events, function(that) {
				return that.id === event.undoneEventId;
			});
			if(undoneEvent.type === 'StartPreparation') {
				accountLocal.reset();
			} else if(undoneEvent.type === 'AddParticipant') {
				var position = this.slots.indexOf(undoneEvent);
				this.slots[position] = new EmtpySlot(position);
			} else if(undoneEvent.type === 'StartBattle') {
				this.warPhase = 'preparation'; 
			} else if(undoneEvent.type === 'AddGuess') {
				this.guesses.splice(this.guesses.indexOf(undoneEvent), 1);
			} else if(undoneEvent.type === 'AddDefense' || undoneEvent.type === 'AddAttack' ) {
				this.battleHistory.splice(this.battleHistory.indexOf(undoneEvent), 1);
			} else if(undoneEvent.type === 'EndWar') {
				this.warPhase = 'battle';
			}
		}
	};
	
	var accountLocalInitializer = function(account) {
		if (account) {
			return
				$http.post('/war/central/getWarState', { actor : account.tag })
				.success(function(response) {
					var nonUndoneWarEventsSinceLastInit = response.sort(earlyerInstantFirst);
					var warState = new WarState();
					angular.forEach(nonUndoneWarEventsSinceLastInit, function(event) {
						warState.digest(event);
					});
					return warState;
				})
				.error(function(reason) {
					throw reason;
				});
		} else 
			return $q.reject('No account was selected');
	};

	var accountLocal = commonSrv.createAccountLocal(accountLocalInitializer);

	// Public API here
	return {
		getWarState: function() {
			return accountLocal.get()
			.then(function(warState) {
				return $http.post('/war/central/getWarEventsAfter', {
					actor: accountLocal.account,
					eventInstant: warState.events[warState.events.length-1].instant
				}).success(function(response) {
					var newEvents = response.sort(earlyerInstantFirst);
					angular.forEach(newEvents, function(event) {
						warState.digest(event);
					});
					return warState;
				}).error(function(reason) {
					throw reason;
				});
			});
		}
	};
});
