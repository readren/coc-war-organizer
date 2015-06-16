#Evolutions

# --- !Ups

CREATE TYPE role_code AS ENUM ('L', 'C', 'V', 'N');

CREATE TABLE orga_account (
	user_id 			UUID NOT NULL REFERENCES auth_user  ON DELETE CASCADE,
	tag					SMALLSERIAL,
	PRIMARY KEY (user_id, tag),
	
	name				TEXT NOT NULL,
	UNIQUE (user_id, name),
	
	description			TEXT
);

CREATE TABLE orga_event (
	id					BIGSERIAL PRIMARY KEY,
	instant				TIMESTAMP NOT NULL,
	med_fk1				INTEGER DEFAULT NULL,
	med_fk2				INTEGER DEFAULT NULL,
	-- big_fk				BIGINT DEFAULT NULL,
	user_id				UUID DEFAULT NULL
);

CREATE TABLE orga_organization ( 
	id					SERIAL PRIMARY KEY,
	clan_name			TEXT NOT NULL,
	clan_tag			TEXT NOT NULL,
	description			TEXT DEFAULT NULL,
	UNIQUE( clan_tag, clan_name, description)
);

-- Represents a member icon. Member icons are owned by the organization, but can only be used by the account for which it was created. If an account that has left the organization joins back, he uses the same member icon he had used before. 
CREATE TABLE orga_icon (
	organization_id			INTEGER NOT NULL  REFERENCES orga_organization  ON DELETE CASCADE,
	tag						SERIAL,
	PRIMARY KEY (organization_id, tag),
	
	name					TEXT NOT NULL,
	present_role			role_code NOT NULL,
	
	holder_user_id			UUID,	-- remembers who is/was the account that is using/has used this member icon in order to be used by the referenced account if he comes back
	holder_account_tag		SMALLINT,
	FOREIGN KEY (holder_user_id, holder_account_tag) REFERENCES orga_account  ON DELETE SET NULL,	-- the member icon remains even after the holder account is deleted

	/* this is another entity that is embedded here for efficiency. The cost is that only the last role change is remembered  */
	role_change_event_id	BIGINT DEFAULT NULL, -- REFERENCES orga_event. For efficiency sake, when this column is updated, and indexed key of this table should be copied to the referenced orga_event row (organization_id -> med_fk1, tag -> med_fk2).
	previous_role			role_code DEFAULT NULL,
	changer_icon_tag		INTEGER DEFAULT NULL,
	FOREIGN KEY (organization_id, changer_icon_tag) REFERENCES orga_icon ON DELETE NO ACTION  
);

-- CREATE TABLE orga_role (
-- 	change_event_id		BIGINT PRIMARY KEY,
-- 	next_change			BIGINT REFERENCES orga_role ON DELETE CASCADE,

-- 	organization_id		INTEGER NOT NULL REFERENCES orga_organization ON DELETE CASCADE,
-- 	icon_tag 			INTEGER NOT NULL,
-- 	FOREIGN KEY (organization_id, tag)
-- );

/** Represents a join request. An account can have at most one active join request. With active I mean: not canceled. */
CREATE TABLE orga_join_request (
	user_id				UUID NOT NULL,
	account_tag			SMALLINT NOT NULL,
	FOREIGN KEY (user_id, account_tag) REFERENCES orga_account  ON DELETE CASCADE,
	request_event_id	BIGINT PRIMARY KEY, -- REFERENCES orga_event
	UNIQUE (user_id, account_tag, cancel_event_id),
	organization_id		INTEGER NOT NULL  REFERENCES orga_organization  ON DELETE CASCADE,


	/* this is another entity that is embedded here for efficiency  */
	cancel_event_id		BIGINT DEFAULT NULL, -- REFERENCES orga_event, id of the "cancel join request" event.  For efficiency sake, when this column is updated, and indexed key of this table should be copied to the referenced orga_event row (user_id -> user_id, account_tag -> med_fk1).
	because_accepted	BOOLEAN DEFAULT NULL, -- tells if the request was canceled because the account was accepted or the account canceled the join request

	/* this is another entity that is embedded here for efficiency  */
	rejection_msg		TEXT DEFAULT NULL, -- the message that the rejecter has written when he rejected the join request
	rejection_event_id	BIGINT DEFAULT NULL, -- REFERENCES orga_event -- id of the rejection event. For efficiency sake, when this column is updated, and indexed key of this table should be copied to the referenced orga_event row (user_id -> user_id, account_tag -> med_fk1).
	rejecter_icon_tag	INTEGER DEFAULT NULL, -- tag of the member icon that rejected the join request
	FOREIGN KEY (organization_id, rejecter_icon_tag) REFERENCES orga_icon ON DELETE CASCADE 
);

/** One to one relationship between an account and its current member icon. */
CREATE TABLE orga_membership (
	organization_id		INTEGER NOT NULL,
	icon_tag			INTEGER NOT NULL,
	FOREIGN KEY (organization_id, icon_tag) REFERENCES orga_icon  ON DELETE CASCADE,
	UNIQUE (organization_id, icon_tag, abandon_event_id),

	user_id				UUID NOT NULL,
	account_tag			SMALLINT NOT NULL,
	FOREIGN KEY (user_id, account_tag) REFERENCES orga_account  ON DELETE CASCADE,
	UNIQUE (user_id, account_tag, abandon_event_id),
	

	request_event_id	BIGINT NOT NULL, -- REFERENCES orga_event	-- id of the join request that was accepted
	accepted_event_id	BIGINT PRIMARY KEY, -- REFERENCES orga_event	-- id of this join request acceptance event
	accepter_icon_tag 	INTEGER NOT NULL,	-- tag of the member icon that accepted the join request
	FOREIGN KEY (organization_id, accepter_icon_tag) REFERENCES orga_icon ON DELETE CASCADE, 

	/* this is another entity that is embedded here for efficiency  */
	abandon_event_id	BIGINT -- REFERENCES orga_event	-- id of the "abandon organization" event.  For efficiency sake, when this column is updated, and indexed key of this table should be copied to the referenced orga_event row (user_id -> user_id, account_tag -> med_fk1).
);


# --- !Downs

DROP TABLE IF EXISTS orga_membership;
DROP TABLE IF EXISTS orga_join_request;
DROP TABLE IF EXISTS orga_icon;
DROP TABLE IF EXISTS orga_organization;
DROP TABLE IF EXISTS orga_event;
DROP TABLE IF EXISTS orga_account;
DROP TYPE IF EXISTS role_code;


