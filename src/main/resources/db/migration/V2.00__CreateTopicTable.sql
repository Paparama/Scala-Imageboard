
create table if not exists topics(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     name text CONSTRAINT namechk CHECK (char_length(name) > 2 and char_length(name) < 21),
                                     board_id bigint REFERENCES boards (id) ON DELETE CASCADE NOT NULL,
                                     last_msg_created_time TIMESTAMP,
                                     UNIQUE(name, board_id)
);