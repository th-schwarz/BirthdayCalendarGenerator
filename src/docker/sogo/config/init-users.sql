CREATE TABLE IF NOT EXISTS sogo_users (
  c_uid VARCHAR(255) NOT NULL PRIMARY KEY,
  c_name VARCHAR(255) NOT NULL,
  c_password VARCHAR(255) NOT NULL,
  c_cn VARCHAR(255),
  mail VARCHAR(255)
);

INSERT INTO sogo_users (c_uid, c_name, c_password, c_cn, mail)
VALUES
  ('dev-user', 'dev-user', 'test123', 'Test User', 'test@example.com'),
  ('admin', 'admin', 'admin', 'Administrator', 'admin@example.com')
ON DUPLICATE KEY UPDATE c_uid=c_uid;
