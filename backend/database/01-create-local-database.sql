-- Reference template. Run equivalent commands locally as the MySQL root user.
-- Do not edit this tracked file with a real password.

CREATE DATABASE IF NOT EXISTS offerflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'offerflow_app'@'localhost'
  IDENTIFIED BY 'REPLACE_WITH_A_STRONG_LOCAL_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
  ON offerflow.* TO 'offerflow_app'@'localhost';

FLUSH PRIVILEGES;
