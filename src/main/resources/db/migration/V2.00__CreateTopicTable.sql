
create table if not exists topics(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     name text not null,
                                     board_id bigint not null,
                                     last_msg_created_time TIMESTAMP,
                                     UNIQUE(name)
);