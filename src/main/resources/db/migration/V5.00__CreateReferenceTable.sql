create table if not exists post_references(
                                              id BIGSERIAL PRIMARY KEY NOT NULL,
                                              reference_to bigint NOT NULL,
                                              post_id bigint REFERENCES posts(id) ON DELETE CASCADE NOT NULL,
                                              text text CONSTRAINT textCheck CHECK (char_length(text) > 2 and char_length(text) < 1000)
);