-- init.sql
CREATE DATABASE IF NOT EXISTS photoservice_db;
ALTER USER 'root'@'%' IDENTIFIED BY 'rootpadsds12ssword';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;
