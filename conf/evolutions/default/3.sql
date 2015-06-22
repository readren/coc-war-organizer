#Evolutions

# --- !Ups


CREATE TABLE war_event (
	id 							BIGSERIAL PRIMARY KEY,
	instant						TIMESTAMP,
	foreign_key					BIGINT
);

CREATE TABLE war_clash (
	clash_start_event_id 		BIGINT PRIMARY KEY, -- clash_id -- REFERENCES war_event
	battle_start_event_id		BIGINT DEFAULT NULL, -- REFERENCES war_event
	clash_end_event_id			BIGINT DEFAULT NULL, -- REFERENCES war_event -- this event id coincides with the clash_start_event_id of the immediate following clash

	organization_id				INTEGER NOT NULL REFERENCES orga_organization  ON DELETE CASCADE,
	enemy_clan_name				TEXT,
	enemy_clan_tag				TEXT,
	UNIQUE(organization_id, clash_end_event_id)
);


CREATE TABLE war_participant (
	add_event_id				BIGINT PRIMARY KEY, -- participant_id  -- REFERENCES war_event
	remove_event_id				BIGINT DEFAULT NULL, -- REFERENCES war_event

	clash_id		 			BIGINT NOT NULL REFERENCES war_clash ON DELETE CASCADE,
	member_tag					INTEGER NOT NULL,
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

	clash_id					BIGINT NOT NULL REFERENCES war_clash ON DELETE CASCADE,
	participant_id 				BIGINT NOT NULL REFERENCES war_participant ON DELETE CASCADE,
	opponent_position 			SMALLINT NOT NULL,
	kind						BOOLEAN NOT NULL, -- true - > attack , false - > defense
	suwe						INTEGER NOT NULL, -- seconds until war end.
	stars 						SMALLINT NOT NULL, -- number of achieved stars
	destruction 				SMALLINT NOT NULL, -- % of destruction
	UNIQUE(clash_id, participant_id, opponent_position, kind, remove_event_id)
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
DROP TABLE IF EXISTS war_clash;
DROP TABLE IF EXISTS war_event;


