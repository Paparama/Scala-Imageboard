create table if not exists posts(
                                    id BIGSERIAL PRIMARY KEY NOT NULL,
                                    text text NOT NULL,
                                    created_at TIMESTAMP NOT NULL,
                                    topic_id bigint REFERENCES topics(id) ON DELETE CASCADE
);