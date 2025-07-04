CREATE TABLE IF NOT EXISTS analyticfinder
(
    id                 bigint not null,
    datajson           jsonb,
    guid               varchar(255),
    time               timestamp,
    root_id            bigint not null,
    PRIMARY KEY (time, id)
) PARTITION BY RANGE (time);
