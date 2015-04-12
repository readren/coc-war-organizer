#Evolutions

# --- !Ups

create table auth_user (
	user_id UUID primary key,
	
	provider_id		text not null, 			-- loginInfo.providerId
	provider_key	text not null, 			-- loginInfo.providerKey
	unique(provider_key, provider_id),
	
	first_name		text,
	last_name		text,
	full_name		text,
	email 			text,
	avatar_url 		text
);

create table auth_oauth1_info (
	provider_id		text not null, 			-- loginInfo.providerId: String
	provider_key	text not null, 			-- loginInfo.providerKey: String
	primary key(provider_key, provider_id),
	
	token			text not null,			-- token: String
	secret			text not null 			-- secret: String
);


create table auth_oauth2_info (
	provider_id 	text not null, 			-- loginInfo.providerId: String
	provider_key 	text not null, 			-- loginInfo.providerKey: String
	primary key(provider_key, provider_id),
	
	access_token 	text not null, 			-- accessToken: String,
	token_type 		text, 					-- tokenType: Option[String] = None,
 	expires_in 		integer, 				-- expiresIn: Option[Int] = None,
 	refresh_token 	text, 					-- refreshToken: Option[String] = None,
	params			text					-- params: Option[Map[String, String]] = None
);

create table auth_password_info (
	provider_id 	text not null, 			-- loginInfo.providerId: String
	provider_key 	text not null, 			-- loginInfo.providerKey: String
	primary key(provider_key, provider_id),
	
	hasher			text not null, 			-- hasher: String,
	password		text not null,			-- password: String,
	salt			text					-- salt: Option[String] = None
);


# --- !Downs

drop table auth_user;
drop table auth_oauth1_info;
drop table auth_oauth2_info;
drop table auth_password_info;
