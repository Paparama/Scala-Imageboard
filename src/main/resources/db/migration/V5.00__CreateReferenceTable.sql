create table if not exists post_references(
                                              id BIGSERIAL PRIMARY KEY NOT NULL,
                                              reference_to bigint NOT NULL,
                                              post_id bigint REFERENCES posts(id) ON DELETE CASCADE NOT NULL,
                                              text text
);