create table if not exists boards(
                                     id BIGSERIAL PRIMARY KEY NOT NULL,
                                     name text CONSTRAINT namechk CHECK (char_length(name) > 2 and char_length(name) < 21),
                                     UNIQUE(name)
);