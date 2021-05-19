create table if not exists images(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     path text not null,
                                     post_id bigint not null
);