#Evolutions

# --- !Ups

create table "auth_user" (
	user_id UUID primary key,
	provider_id text not null unique,
	provider_key text not null,
	first_name text,
	last_name text,
	full_name text,
	email text,
	avatar_url text
);


# --- !Downs

drop table auth_user;
