#Evolutions

# --- !Ups

CREATE TYPE role_code AS ENUM ('L', 'C', 'V', 'N');

CREATE TABLE orga_account (
	user_id 			UUID NOT NULL REFERENCES auth_user  ON DELETE CASCADE  ON UPDATE RESTRICT,
	tag					SMALLSERIAL,
	PRIMARY KEY (user_id, tag),
	
	name				TEXT NOT NULL,
	UNIQUE (user_id, name),
	
	description			TEXT
);

CREATE TABLE orga_event (
	id					BIGSERIAL PRIMARY KEY,
	instant				TIMESTAMP
);

CREATE TABLE orga_organization (
	id					UUID  PRIMARY KEY,
	clan_name			TEXT NOT NULL,
	clan_tag			TEXT NOT NULL,
	description			TEXT,
	UNIQUE( clan_tag, clan_name, description)
);

-- Represents a member icon. Member icons are owned by the organization, but can only be used by the account for which it was created. If an account that has left the organization joins back, he uses the same member icon he had used before. 
CREATE TABLE orga_member (
	organization_id		UUID NOT NULL  REFERENCES orga_organization  ON DELETE CASCADE  ON UPDATE RESTRICT,
	tag					SERIAL,
	PRIMARY KEY (organization_id, tag),
	
	name				TEXT NOT NULL,
	role				role_code NOT NULL,
	
	holder_user_id		UUID,																							-- remembers who is/was the account that is using/has used this member icon in order to be used by the referenced account if he comes back
	holder_account_tag	SMALLINT,
	FOREIGN KEY (holder_user_id, holder_account_tag) REFERENCES orga_account  ON DELETE SET NULL  ON UPDATE RESTRICT	-- the member icon remains even after the holder account is deleted
);

/**Represents a join request. An account can have at most one active join request. With active I mean: not canceled. */
CREATE TABLE orga_join_request (
	user_id				UUID,
	account_tag			SMALLINT,
	FOREIGN KEY (user_id, account_tag) REFERENCES orga_account  ON DELETE CASCADE  ON UPDATE RESTRICT,
	request_event_id	BIGINT NOT NULL  REFERENCES orga_event  ON DELETE CASCADE  ON UPDATE RESTRICT,
	UNIQUE (user_id, account_tag, cancel_event_id),
	organization_id		UUID NOT NULL  REFERENCES orga_organization  ON DELETE CASCADE  ON UPDATE RESTRICT,			

	/* this another entity that is embedded here for efficiency  */
	cancel_event_id		BIGINT REFERENCES orga_event  ON DELETE CASCADE  ON UPDATE RESTRICT,							-- id of the "cancel join request" event

	/* this another entity that is embedded here for efficiency  */
	rejection_msg		TEXT,																							-- the message that the rejecter has written when he rejected the join request
	rejection_event_id	BIGINT  REFERENCES orga_event  ON DELETE CASCADE  ON UPDATE RESTRICT,							-- id of the join request rejection 
	rejecter_member_tag	INTEGER,																						-- tag of the member that rejected the join request
	FOREIGN KEY (organization_id, rejecter_member_tag) REFERENCES orga_member ON DELETE CASCADE ON UPDATE RESTRICT
);

CREATE TABLE orga_membership (
	organization_id		UUID,
	member_tag			INTEGER,
	FOREIGN KEY (organization_id, member_tag) REFERENCES orga_member  ON DELETE CASCADE  ON UPDATE RESTRICT,

	user_id				UUID,
	account_tag			SMALLINT,
	FOREIGN KEY (user_id, account_tag) REFERENCES orga_account  ON DELETE CASCADE  ON UPDATE RESTRICT,
	
	UNIQUE (organization_id, member_tag, user_id, account_tag, abandon_event_id),

	request_event_id	BIGINT REFERENCES orga_event  ON DELETE SET NULL  ON UPDATE RESTRICT, 							-- id of the join request that was accepted
	accepted_event_id	BIGINT REFERENCES orga_event  ON DELETE SET NULL  ON UPDATE RESTRICT, 							-- id of this join request acceptance event
	accepter_member_tag INTEGER NOT NULL,																				-- tag of the member that accepted the join request
	FOREIGN KEY (organization_id, accepter_member_tag) REFERENCES orga_member ON DELETE CASCADE ON UPDATE RESTRICT, 

	/* this another entity that is embedded here for efficiency  */
	abandon_event_id	BIGINT REFERENCES orga_event ON DELETE CASCADE  ON UPDATE RESTRICT								-- id of the "abandon organization" event
);


# --- !Downs

DROP TABLE IF EXISTS orga_membership;
DROP TABLE IF EXISTS orga_join_request;
DROP TABLE IF EXISTS orga_member;
DROP TABLE IF EXISTS orga_account;
DROP TABLE IF EXISTS orga_organization;
DROP TABLE IF EXISTS orga_event;
DROP TYPE IF EXISTS role_code;


