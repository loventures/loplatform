CREATE OR REPLACE FUNCTION fts(
  content TSVECTOR,
  search CHARACTER VARYING
) RETURNS BOOLEAN AS
$$
    BEGIN
        RETURN $1 @@ plainto_tsquery('english', $2);
    END ;
$$ LANGUAGE plpgsql;
