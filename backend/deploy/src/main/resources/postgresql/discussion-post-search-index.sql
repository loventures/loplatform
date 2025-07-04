CREATE INDEX CONCURRENTLY IF NOT EXISTS post_searchablecontent_index ON discussionpost USING gin(searchablecontent);
