create table if not exists posts(
                                    id BIGSERIAL PRIMARY KEY NOT NULL,
                                    image_ids BIGINT[] not null,
                                    text text not null,
                                    created_at TIMESTAMP not null,
                                    references_responses BIGINT[] not null,
                                    references_from BIGINT[] not null,
                                    topic_id bigint
);