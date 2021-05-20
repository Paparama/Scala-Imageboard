CREATE OR REPLACE FUNCTION delete_on_insert() RETURNS trigger
AS $$
BEGIN
    DELETE FROM topics
    WHERE (
              SELECT count(*)
              FROM topics t
                       JOIN posts p
                            ON t.id = p.topic_id
              WHERE t.id = new.topic_id) > 999
      AND topics.id = new.topic_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trig_delete_topic
    AFTER INSERT ON posts
    FOR EACH ROW
EXECUTE PROCEDURE delete_on_insert();