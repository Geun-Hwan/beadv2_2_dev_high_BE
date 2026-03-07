-- Spring Batch metadata for table-prefix: public.batch_
-- Matches config: spring.batch.jdbc.table-prefix=public.batch_

CREATE SEQUENCE IF NOT EXISTS public.batch_job_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS public.batch_job_execution_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS public.batch_step_execution_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS public.batch_job_instance (
    job_instance_id BIGINT PRIMARY KEY,
    version BIGINT,
    job_name VARCHAR(100) NOT NULL,
    job_key VARCHAR(32) NOT NULL,
    CONSTRAINT batch_job_inst_un UNIQUE (job_name, job_key)
);

CREATE TABLE IF NOT EXISTS public.batch_job_execution (
    job_execution_id BIGINT PRIMARY KEY,
    version BIGINT,
    job_instance_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    status VARCHAR(10),
    exit_code VARCHAR(2500),
    exit_message VARCHAR(2500),
    last_updated TIMESTAMP,
    CONSTRAINT batch_job_exec_fk FOREIGN KEY (job_instance_id)
        REFERENCES public.batch_job_instance(job_instance_id)
);

CREATE TABLE IF NOT EXISTS public.batch_job_execution_params (
    job_execution_id BIGINT NOT NULL,
    parameter_name VARCHAR(100) NOT NULL,
    parameter_type VARCHAR(100) NOT NULL,
    parameter_value VARCHAR(2500),
    identifying CHAR(1) NOT NULL,
    CONSTRAINT batch_job_exec_params_fk FOREIGN KEY (job_execution_id)
        REFERENCES public.batch_job_execution(job_execution_id)
);

CREATE TABLE IF NOT EXISTS public.batch_step_execution (
    step_execution_id BIGINT PRIMARY KEY,
    version BIGINT NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    job_execution_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    status VARCHAR(10),
    commit_count BIGINT,
    read_count BIGINT,
    filter_count BIGINT,
    write_count BIGINT,
    read_skip_count BIGINT,
    write_skip_count BIGINT,
    process_skip_count BIGINT,
    rollback_count BIGINT,
    exit_code VARCHAR(2500),
    exit_message VARCHAR(2500),
    last_updated TIMESTAMP,
    CONSTRAINT batch_step_exec_fk FOREIGN KEY (job_execution_id)
        REFERENCES public.batch_job_execution(job_execution_id)
);

CREATE TABLE IF NOT EXISTS public.batch_step_execution_context (
    step_execution_id BIGINT PRIMARY KEY,
    short_context VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT batch_step_exec_ctx_fk FOREIGN KEY (step_execution_id)
        REFERENCES public.batch_step_execution(step_execution_id)
);

CREATE TABLE IF NOT EXISTS public.batch_job_execution_context (
    job_execution_id BIGINT PRIMARY KEY,
    short_context VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT batch_job_exec_ctx_fk FOREIGN KEY (job_execution_id)
        REFERENCES public.batch_job_execution(job_execution_id)
);

CREATE INDEX IF NOT EXISTS batch_job_inst_idx ON public.batch_job_instance(job_name);
CREATE INDEX IF NOT EXISTS batch_job_exec_inst_idx ON public.batch_job_execution(job_instance_id);
CREATE INDEX IF NOT EXISTS batch_step_exec_job_idx ON public.batch_step_execution(job_execution_id);
