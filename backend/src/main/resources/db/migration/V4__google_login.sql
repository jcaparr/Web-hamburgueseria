-- Google's stable identifier for the account ("sub" in the ID token). The identity is
-- anchored on it rather than on the email, because a person can change the address on
-- their Google account and we would otherwise hand them a different account, or worse,
-- hand someone else theirs.
alter table users add column google_sub varchar(64);
alter table users add constraint uk_users_google_sub unique (google_sub);

-- Someone who only ever signs in with Google has no password. It was not null until
-- now because every account had one.
alter table users alter column password_hash drop not null;

-- An account has to be reachable one way or the other.
alter table users add constraint ck_users_has_credential
    check (password_hash is not null or google_sub is not null);

-- Linking a Google account to an existing password account is confirmed by email,
-- like every other sensitive change.
alter table verification_codes drop constraint ck_verification_codes_purpose;
alter table verification_codes add constraint ck_verification_codes_purpose
    check (purpose in ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'GOOGLE_LINK'));
