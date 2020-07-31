drop table if exists ID_GENERATOR;
drop table if exists AGENCY;
drop table if exists WCTUSER;
drop table if exists USER_ROLE;
create table ID_GENERATOR ( IG_TYPE varchar(255),  IG_VALUE integer );
create table AGENCY (AGC_OID bigint not null, AGC_NAME varchar(80) not null unique, AGC_ADDRESS varchar(255) not null, AGC_LOGO_URL varchar(255), AGC_URL varchar(255), AGC_EMAIL varchar(80), AGC_FAX varchar(20), AGC_PHONE varchar(20), AGC_SHOW_TASKS boolean not null default true, AGC_DEFAULT_DESC_TYPE varchar(50) default '', primary key (AGC_OID));
create table WCTUSER (USR_OID bigint not null, USR_ACTIVE bit not null, USR_ADDRESS varchar(200), USR_EMAIL varchar(100) not null, USR_EXTERNAL_AUTH bit not null, USR_FIRSTNAME varchar(50) not null, USR_FORCE_PWD_CHANGE bit not null, USR_LASTNAME varchar(50) not null, USR_NOTIFICATIONS_BY_EMAIL bit not null, USR_PASSWORD varchar(255), USR_PHONE varchar(16), USR_TITLE varchar(10), USR_USERNAME varchar(80) not null unique, USR_AGC_OID bigint not null, USR_DEACTIVATE_DATE TIMESTAMP NULL, USR_TASKS_BY_EMAIL bit not null, USR_NOTIFY_ON_GENERAL bit not null, USR_NOTIFY_ON_WARNINGS bit not null, primary key (USR_OID));
create table USER_ROLE (URO_ROL_OID bigint not null, URO_USR_OID bigint not null, primary key (URO_USR_OID, URO_ROL_OID));
