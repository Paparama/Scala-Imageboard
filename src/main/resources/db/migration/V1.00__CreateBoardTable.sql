create table if not exists boards(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     name text not null,
                                     topics bigint[],
                                     UNIQUE(name)
);