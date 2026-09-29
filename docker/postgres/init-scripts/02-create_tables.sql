-- Создаем таблицы если они не существуют
\c wise_task_profile
CREATE TABLE IF NOT EXISTS profile (
    id uuid not null,
    email varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    patronymic varchar(255),
    profile_password varchar(255),
    profile_role varchar(255),
    student_group varchar(255),
    primary key (id)
);

CREATE TABLE IF NOT EXISTS password_recovery (
    recovery_token uuid not null,
    email varchar(255),
    created_at timestamp(6),
    expires_at timestamp(6),
    primary key (recovery_token)
);

\c wise_task_task
CREATE TABLE IF NOT EXISTS catalog (
    author_id uuid,
    id uuid not null,
    catalog varchar(255),
    name varchar(255),
    primary key (id)
);

CREATE TABLE IF NOT EXISTS catalog_student_ids (
    catalog_id uuid not null,
    student_ids uuid
);

CREATE TABLE IF NOT EXISTS solution (
    is_correct boolean,
    author_id uuid,
    id uuid not null,
    task_id uuid,
    primary key (id)
);

CREATE TABLE IF NOT EXISTS solution_graph (
    graph_id uuid,
    id uuid not null,
    result jsonb,
    primary key (id)
);

CREATE TABLE IF NOT EXISTS solution_implementation (
    id uuid not null,
    code varchar(255),
    implementation_result jsonb,
    primary key (id)
);

CREATE TABLE IF NOT EXISTS task (
    is_public boolean,
    author_id uuid,
    id uuid not null,
    category varchar(255),
    description varchar(255),
    name varchar(255),
    task_type varchar(255) check (task_type in ('IMPLEMENTATION','GRAPH')),
    primary key (id)
);

CREATE TABLE IF NOT EXISTS task_catalog (
    catalog_id uuid not null,
    task_id uuid
);

CREATE TABLE IF NOT EXISTS task_graph (
    is_hidden_mistake boolean,
    graph_id uuid,
    id uuid not null,
    condition jsonb,
    rule jsonb,
    primary key (id)
);

CREATE TABLE IF NOT EXISTS task_implementation (
    id uuid not null,
    plugin_id uuid,
    primary key (id)
);

-- Осторожно с foreign keys - они могут уже существовать
DO $$
BEGIN
    -- Добавляем constraints только если их нет
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'FK3vp0aj24cls00qfbw43x0175d') THEN
        ALTER TABLE catalog_student_ids ADD CONSTRAINT FK3vp0aj24cls00qfbw43x0175d FOREIGN KEY (catalog_id) REFERENCES catalog;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'FK9j8f7ya0we8l9d1qgxhb2sxwr') THEN
        ALTER TABLE solution_graph ADD CONSTRAINT FK9j8f7ya0we8l9d1qgxhb2sxwr FOREIGN KEY (id) REFERENCES solution;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'FKpouwsj45b4ua1eobf6v3qghg7') THEN
        ALTER TABLE solution_implementation ADD CONSTRAINT FKpouwsj45b4ua1eobf6v3qghg7 FOREIGN KEY (id) REFERENCES solution;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'FK2x5vv3las4inaro4ow57w4ahm') THEN
        ALTER TABLE task_catalog ADD CONSTRAINT FK2x5vv3las4inaro4ow57w4ahm FOREIGN KEY (catalog_id) REFERENCES catalog;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'FK4irpkxn11fha2kvb3bxivxtyg') THEN
        ALTER TABLE task_graph ADD CONSTRAINT FK4irpkxn11fha2kvb3bxivxtyg FOREIGN KEY (id) REFERENCES task;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'FKpx6vuo2o6t97sux59dbbhdvca') THEN
        ALTER TABLE task_implementation ADD CONSTRAINT FKpx6vuo2o6t97sux59dbbhdvca FOREIGN KEY (id) REFERENCES task;
    END IF;
END $$;

\c wise_task_plugin
CREATE TABLE IF NOT EXISTS plugin (
    id uuid not null,
    author_id uuid,
    bean_name varchar(255),
    category varchar(255),
    description varchar(255),
    graph_type varchar(255),
    is_internal boolean,
    is_valid boolean,
    jar_file bytea,
    jar_name varchar(255),
    name varchar(255),
    plugin_type varchar(255),
    primary key (id)
);
