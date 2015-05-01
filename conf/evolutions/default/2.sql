#Evolutions

# --- !Ups

create table orga_account (
	user_id 		UUID not null REFERENCES auth_user (user_id) on delete cascade on update restrict,
	name			text not null,
	primary key(user_id, name),
	
	description		text
);



# --- !Downs

drop orga_account;
