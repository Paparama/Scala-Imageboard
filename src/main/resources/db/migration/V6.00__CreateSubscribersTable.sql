create table if not exists subscribers(
                                              id BIGSERIAL PRIMARY KEY NOT NULL,
                                              topic_id bigint REFERENCES topics(id) ON DELETE CASCADE NOT NULL,
                                              email text,
                                              UNIQUE(email, topic_id)
);