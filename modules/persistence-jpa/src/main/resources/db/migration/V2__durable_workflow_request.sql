create table workflow_request (
    task_id varchar(80) primary key,
    job_description text not null,
    created_at timestamp with time zone not null
);
