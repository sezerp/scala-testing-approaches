-- accounts schema
CREATE TABLE IF NOT EXISTS "accounts"
(
    "id"   UUID NOT NULL,
    "name" TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE "accounts"
    ADD CONSTRAINT "accounts_pk" PRIMARY KEY ("id");

-- users schema
CREATE TABLE IF NOT EXISTS "users"
(
    "id"              UUID        NOT NULL,
    "account_id"      UUID        NOT NULL,
    "login"           TEXT        NOT NULL,
    "role"            TEXT        NOT NULL,
    "email_lowercase" TEXT        NOT NULL,
    "login_lowercase" TEXT        NOT NULL,
    "password"        TEXT        NOT NULL,
    "created_on"      TIMESTAMPTZ NOT NULL
);

ALTER TABLE "users"
    ADD CONSTRAINT "users_pk" PRIMARY KEY ("id");
CREATE UNIQUE INDEX "users_login_lowercase_ak" ON "users" ("login_lowercase");
CREATE UNIQUE INDEX "users_email_lowercase_ak" ON "users" ("email_lowercase");
ALTER TABLE "users"
    ADD CONSTRAINT "accounts_fk" FOREIGN KEY ("account_id") REFERENCES "accounts" ("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- api_keys schema
CREATE TABLE "api_keys"
(
    "id"          UUID        NOT NULL,
    "user_id"     UUID        NOT NULL,
    "valid_until" TIMESTAMPTZ NOT NULL,
    "created_on"  TIMESTAMPTZ NOT NULL
);

ALTER TABLE "api_keys"
    ADD CONSTRAINT "api_keys_pk" PRIMARY KEY ("id");
ALTER TABLE "api_keys"
    ADD CONSTRAINT "users_fk" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE ON UPDATE CASCADE;