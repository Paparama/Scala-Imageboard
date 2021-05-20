create table if not exists boards(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     name text NOT NULL,
                                     UNIQUE(name)
);