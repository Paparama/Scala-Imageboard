create table if not exists boards(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     name text not null,
                                     topic_ids bigint[],
                                     UNIQUE(name)
);