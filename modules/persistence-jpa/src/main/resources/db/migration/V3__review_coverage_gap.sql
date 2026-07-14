create table review_coverage_gap (
    gap_id varchar(200) primary key,
    task_id varchar(80) not null,
    ordinal_no integer not null,
    gap_json text not null
);

create unique index idx_review_gap_task_order
    on review_coverage_gap(task_id, ordinal_no);
