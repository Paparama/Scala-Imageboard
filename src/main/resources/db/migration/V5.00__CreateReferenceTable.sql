create table if not exists post_references(
                                              id BIGSERIAL PRIMARY KEY NOT NULL,
                                              reference_to bigint not null,
                                              post_id bigint not null,
                                              text text
);