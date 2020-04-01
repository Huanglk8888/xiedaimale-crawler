create table NEWS
(
    ID          BIGINT auto_increment
        primary key,
    TITLE       TEXT,
    CONTENT     TEXT,
    URL         VARCHAR(100),
    CREATED_AT  DATETIME,
    MODIFIED_AT DATETIME
);

create table LINKS_TO_BE_PROCESSED (
    LINK VARCHAR(2000)
);

create table LINKS_ALREADY_PROCESSED (
    LINK VARCHAR(2000)
);