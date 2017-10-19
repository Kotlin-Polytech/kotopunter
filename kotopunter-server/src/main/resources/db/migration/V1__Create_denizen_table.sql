CREATE TABLE IF NOT EXISTS denizen_unsafe (
  id SERIAL NOT NULL PRIMARY KEY,
  denizen_id TEXT UNIQUE,
  password TEXT,
  salt TEXT,
  email TEXT UNIQUE
);

DROP VIEW IF EXISTS denizen CASCADE;
CREATE VIEW denizen AS SELECT id, denizen_id, email FROM denizen_unsafe;
