create table if not exists images(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     path text NOT NULL,
                                     post_id bigint REFERENCES posts(id) NOT NULL
);