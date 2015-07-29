#Evolutions

# --- !Ups


CREATE TABLE war_event (
	id 							BIGSERIAL PRIMARY KEY,
	instant						TIMESTAMP NOT NULL,
	actor_icon_name				TEXT NOT NULL,
	foreign_key					BIGINT DEFAULT NULL
);
CREATE INDEX war_event_instant_index ON war_even(instant); -- TODO do a uniqueTimestamp function to avoid the necesity of the BIGSERIAL and let the instant be the primary key.


-- CREATE TABLE war_clash (
-- 	start_preparation_event_id 	BIGINT PRIMARY KEY, -- clash_id -- REFERENCES war_event
-- 	start_battle_event_id		BIGINT DEFAULT NULL, -- REFERENCES war_event
-- 	end_war_event_id			BIGINT DEFAULT NULL, -- REFERENCES war_event -- this event id coincides with the start_preparation_event_id of the immediate following clash

-- 	start_preparation_orga_event_id 	BIGINT NOT NULL -- REFERENCES orga_event
-- 	organization_id				INTEGER NOT NULL REFERENCES orga_organization  ON DELETE CASCADE,
-- 	enemy_clan_name				TEXT,
-- 	enemy_clan_tag				TEXT,
-- 	UNIQUE(organization_id, end_war_event_id)
-- );

CREATE TABLE war_clash (
	add_event_id 				BIGINT PRIMARY KEY, -- clash_id -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	start_clash_orga_event_id 	BIGINT NOT NULL -- REFERENCES orga_event
	organization_id				INTEGER NOT NULL REFERENCES orga_organization  ON DELETE CASCADE,
	enemy_clan_name				TEXT,
	enemy_clan_tag				TEXT,

	-- mutable fields: no cached query result should depend on the value of this fields. For now, only the getWarPhaseInfo query depends on them.
	current_battle_id			BIGINT DEFAULT NULL -- REFERENCES war_battle. Set when a start battle event is added, and unset when that battle event is undone.
	next_clash_id				BIGINT DEFAULT NULL, -- REFERENCES war_clash. Set when the next war preparation is started and unset when that war is undone.
 	UNIQUE(organization_id, next_clash_id, remove_event_id)
);
--CREATE INDEX war_clash_organization_index ON war_clash(organization_id, remove_event_id);

CREATE TABLE war_battle (
	add_event_id 				BIGINT PRIMARY KEY, -- battle_id  -- REFERENCES war_event
	remove_event_id 			BIGINT DEFAULT NULL, -- REFERENCES war_event
	clash_id		 			BIGINT NOT NULL REFERENCES war_clash ON DELETE CASCADE,
	UNIQUE(clash_id, remove_event_id)

	current_end_id				BIGINT DEFAULT NULL -- REFERENCES war_end. Set when a end war event is added, and unset when that end event is undone.
);

CREATE TABLE war_end (
	add_event_id 				BIGINT PRIMARY KEY, -- end_id  -- REFERENCES war_event
	remove_event_id 			BIGINT DEFAULT NULL, -- REFERENCES war_event
	battle_id		 			BIGINT NOT NULL REFERENCES war_battle ON DELETE CASCADE,
	UNIQUE(battle_id, remove_event_id)
);

CREATE TABLE war_participant (
	add_event_id				BIGINT PRIMARY KEY, -- participant_id  -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	clash_id		 			BIGINT NOT NULL REFERENCES war_clash ON DELETE CASCADE,
	icon_tag					INTEGER NOT NULL,
	base_position				SMALLINT,
	UNIQUE(clash_id, base_position, remove_event_id)
);

CREATE TABLE war_reservation (
	add_event_id				BIGINT PRIMARY KEY, -- war_reservation_id -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	participant_id		 		INTEGER NOT NULL REFERENCES war_participant ON DELETE CASCADE,
	target_position 			SMALLINT NOT NULL,
	UNIQUE(participant_id, target_position, remove_event_id)
);

CREATE TABLE war_guess (
	add_event_id				BIGINT PRIMARY KEY, -- war_guess_id -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	clash_id					BIGINT NOT NULL REFERENCES war_clash ON DELETE CASCADE,
	guesser_member_tag			INTEGER NOT NULL,
	judged_participant_id		BIGINT NOT NULL REFERENCES war_participant ON DELETE CASCADE,
	target_position				SMALLINT NOT NULL,
	one_star_forecast			SMALLINT NOT NULL, -- guesstimate of one star
	two_stars_forecast			SMALLINT NOT NULL, -- guesstimate of two stars
	three_stars_forecast		SMALLINT NOT NULL, -- guesstimate of thres stars
	UNIQUE(clash_id, guesser_member_tag, judged_participant_id, target_position, remove_event_id)
);

CREATE TABLE war_fight (
	add_event_id				BIGINT PRIMARY KEY, -- war_fight_id -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	battle_id					BIGINT NOT NULL REFERENCES war_clash ON DELETE CASCADE,
	participant_position		SMALLINT NOT NULL,
	opponent_position 			SMALLINT NOT NULL,
	kind						BOOLEAN NOT NULL, -- true - > attack , false - > defense
	suwe						INTEGER NOT NULL, -- seconds until war end.
	stars 						SMALLINT NOT NULL, -- number of achieved stars
	destruction 				SMALLINT NOT NULL, -- % of destruction
	UNIQUE(battle_id, participant_id, opponent_position, kind, remove_event_id)
);

CREATE TABLE war_plan (
	add_event_id				BIGINT PRIMARY KEY, -- war_plan_id -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	clash_id					BIGINT NOT NULL REFERENCES war_clash ON DELETE CASCADE,
	participant_id 				BIGINT NOT NULL REFERENCES war_participant ON DELETE CASCADE,
	target_position				SMALLINT,
	attack_number				SMALLINT,
	UNIQUE(clash_id, participant_id, target_position, remove_event_id)
);

CREATE TABLE war_queue (
	add_event_id				BIGINT PRIMARY KEY, -- war_queue_id -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	clash_id					BIGINT REFERENCES war_clash ON DELETE CASCADE,
	target_position				SMALLINT,
	scheddule					INTEGER[], -- each item indicates the time (in seconds until war end) after which the member whose position is equal to the index is allowed to attack. 

	UNIQUE(clash_id, target_position, remove_event_id)
);


# --- !Downs

DROP TABLE IF EXISTS war_queue;
DROP TABLE IF EXISTS war_plan;
DROP TABLE IF EXISTS war_fight;
DROP TABLE IF EXISTS war_guess;
DROP TABLE IF EXISTS war_reservation;
DROP TABLE IF EXISTS war_participant;
DROP TABLE IF EXISTS war_end;
DROP TABLE IF EXISTS war_battle;
DROP TABLE IF EXISTS war_clash;
DROP INDEX IF EXISTS war_event_instant_index;
DROP TABLE IF EXISTS war_event;


