-- Создаем базы данных
CREATE DATABASE wise_task_profile;
CREATE DATABASE wise_task_plugin;
CREATE DATABASE wise_task_task;

-- Создаем отдельных пользователей для каждой БД
CREATE USER wise_task_profile WITH PASSWORD 'wise_task_profile';
CREATE USER wise_task_plugin WITH PASSWORD 'wise_task_plugin';
CREATE USER wise_task_task WITH PASSWORD 'wise_task_task';

-- Даем права общему пользователю
GRANT ALL PRIVILEGES ON DATABASE wise_task_profile TO wise_task;
GRANT ALL PRIVILEGES ON DATABASE wise_task_plugin TO wise_task;
GRANT ALL PRIVILEGES ON DATABASE wise_task_task TO wise_task;

-- Даем права отдельным пользователям
GRANT ALL PRIVILEGES ON DATABASE wise_task_profile TO wise_task_profile;
GRANT ALL PRIVILEGES ON DATABASE wise_task_plugin TO wise_task_plugin;
GRANT ALL PRIVILEGES ON DATABASE wise_task_task TO wise_task_task;