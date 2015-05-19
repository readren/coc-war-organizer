#Evolutions

# --- !Ups

CREATE TABLE orga_account (
	user_id 		UUID NOT NULL REFERENCES auth_user (user_id)  ON DELETE CASCADE  ON UPDATE RESTRICT,
	tag				SMALLSERIAL,
	PRIMARY KEY (user_id, tag),
	
	name			TEXT NOT NULL,
	UNIQUE (user_id, name),
	
	description		TEXT
);

CREATE TABLE orga_organization (
	id				UUID  PRIMARY KEY,
	clan_name		TEXT NOT NULL,
	clan_tag		TEXT NOT NULL,
	description		TEXT,
	UNIQUE( clan_tag, clan_name, description)
);

CREATE TABLE orga_member (
	organization_id	UUID NOT NULL  REFERENCES orga_organization  ON DELETE CASCADE  ON UPDATE RESTRICT,
	tag				SERIAL,
	PRIMARY KEY (organization_id, tag),
	
	name			TEXT NOT NULL
);

CREATE TABLE orga_join_request (
	user_id			UUID,
	account_tag		SMALLINT,
	PRIMARY KEY (user_id, account_tag),
	FOREIGN KEY (user_id, account_tag) REFERENCES orga_account  ON DELETE CASCADE  ON UPDATE RESTRICT,
	
	organization_id	UUID NOT NULL  REFERENCES orga_organization  ON DELETE CASCADE  ON UPDATE RESTRICT,
	rejection_msg	text
);

CREATE TABLE orga_membership (
	organization_id	UUID,
	member_tag		INTEGER,
	FOREIGN KEY (organization_id, member_tag) REFERENCES orga_member  ON DELETE CASCADE  ON UPDATE RESTRICT,

	user_id			UUID,
	account_tag		SMALLINT,
	FOREIGN KEY (user_id, account_tag) REFERENCES orga_account  ON DELETE CASCADE  ON UPDATE RESTRICT,
	
	PRIMARY KEY (organization_id, member_tag, user_id, account_tag)
);


# --- !Downs

DROP TABLE orga_membership;
DROP TABLE orga_join_request;
DROP TABLE orga_member;
DROP TABLE orga_account;
DROP TABLE orga_organization;



