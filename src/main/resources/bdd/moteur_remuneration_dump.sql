--
-- PostgreSQL database dump
--

-- Dumped from database version 16.1
-- Dumped by pg_dump version 16.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: batch_job_execution; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone,
    job_configuration_location character varying(2500)
);


ALTER TABLE public.batch_job_execution OWNER TO postgres;

--
-- Name: batch_job_execution_context; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_execution_context (
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


ALTER TABLE public.batch_job_execution_context OWNER TO postgres;

--
-- Name: batch_job_execution_params; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    type_cd character varying(6) NOT NULL,
    key_name character varying(100) NOT NULL,
    string_val character varying(250),
    date_val timestamp without time zone,
    long_val bigint,
    double_val double precision,
    identifying character(1) NOT NULL
);


ALTER TABLE public.batch_job_execution_params OWNER TO postgres;

--
-- Name: batch_job_execution_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.batch_job_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.batch_job_execution_seq OWNER TO postgres;

--
-- Name: batch_job_instance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_instance (
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL
);


ALTER TABLE public.batch_job_instance OWNER TO postgres;

--
-- Name: batch_job_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.batch_job_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.batch_job_seq OWNER TO postgres;

--
-- Name: batch_step_execution; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_step_execution (
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);


ALTER TABLE public.batch_step_execution OWNER TO postgres;

--
-- Name: batch_step_execution_context; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


ALTER TABLE public.batch_step_execution_context OWNER TO postgres;

--
-- Name: batch_step_execution_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.batch_step_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.batch_step_execution_seq OWNER TO postgres;

--
-- Name: clients_transactions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.clients_transactions (
    id integer NOT NULL,
    id_client character varying(255),
    num_phone character varying(20),
    first_name character varying(255),
    last_name character varying(255),
    fullname character varying(255),
    address text,
    libelle_ville character varying(255),
    code_ville character varying(50),
    gender character(1),
    civility character varying(50),
    nationality character varying(50),
    country_code character varying(10),
    type_id character varying(50),
    num_id character varying(255),
    identity_expirdate date,
    niveau_wallet character varying(255),
    dt_sous_wallet date,
    dt_naissance date,
    age integer,
    rib character varying(255),
    id_client_m2t character varying(255),
    rib_compte_interne character varying(255),
    id_wallet character varying(255),
    dt_entree_relation_m2t date,
    code_oper character varying(50),
    type_transaction character varying(50),
    mnt numeric(19,2),
    date_validation date,
    code_es character varying(50)
);


ALTER TABLE public.clients_transactions OWNER TO postgres;

--
-- Name: clients_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.clients_transactions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.clients_transactions_id_seq OWNER TO postgres;

--
-- Name: clients_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.clients_transactions_id_seq OWNED BY public.clients_transactions.id;


--
-- Name: remuneration; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.remuneration (
    id integer NOT NULL,
    code_es character varying(255) NOT NULL,
    montant numeric(19,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.remuneration OWNER TO postgres;

--
-- Name: remuneration_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.remuneration_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.remuneration_id_seq OWNER TO postgres;

--
-- Name: remuneration_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.remuneration_id_seq OWNED BY public.remuneration.id;


--
-- Name: clients_transactions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clients_transactions ALTER COLUMN id SET DEFAULT nextval('public.clients_transactions_id_seq'::regclass);


--
-- Name: remuneration id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.remuneration ALTER COLUMN id SET DEFAULT nextval('public.remuneration_id_seq'::regclass);


--
-- Data for Name: batch_job_execution; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_execution (job_execution_id, version, job_instance_id, create_time, start_time, end_time, status, exit_code, exit_message, last_updated, job_configuration_location) FROM stdin;
1	2	1	2024-01-21 21:28:35.044	2024-01-21 21:28:35.083	2024-01-21 21:28:35.18	FAILED	FAILED	org.springframework.batch.item.ItemStreamException: Failed to initialize the reader\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader.open(AbstractItemCountingItemStreamItemReader.java:153)\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader$$FastClassBySpringCGLIB$$ebb633d0.invoke(<generated>)\r\n\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:783)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.doProceed(DelegatingIntroductionInterceptor.java:137)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.invoke(DelegatingIntroductionInterceptor.java:124)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:698)\r\n\tat org.springframework.batch.item.database.JpaCursorItemReader$$EnhancerBySpringCGLIB$$2797d295.open(<generated>)\r\n\tat org.springframework.batch.item.support.CompositeItemStream.open(CompositeItemStream.java:104)\r\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.open(TaskletStep.java:311)\r\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:205)\r\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:152)\r\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:413)\r\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:136)\r\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:320)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:149)\r\n\tat org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:140)\r\n\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\r\n\tat java.base/jdk.internal.reflect.Nativ	2024-01-21 21:28:35.18	\N
2	2	2	2024-01-21 21:29:31.936	2024-01-21 21:29:31.979	2024-01-21 21:29:32.069	FAILED	FAILED	org.springframework.batch.item.ItemStreamException: Failed to initialize the reader\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader.open(AbstractItemCountingItemStreamItemReader.java:153)\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader$$FastClassBySpringCGLIB$$ebb633d0.invoke(<generated>)\r\n\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:783)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.doProceed(DelegatingIntroductionInterceptor.java:137)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.invoke(DelegatingIntroductionInterceptor.java:124)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:698)\r\n\tat org.springframework.batch.item.database.JpaCursorItemReader$$EnhancerBySpringCGLIB$$d1167e1.open(<generated>)\r\n\tat org.springframework.batch.item.support.CompositeItemStream.open(CompositeItemStream.java:104)\r\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.open(TaskletStep.java:311)\r\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:205)\r\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:152)\r\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:413)\r\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:136)\r\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:320)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:149)\r\n\tat org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:140)\r\n\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\r\n\tat java.base/jdk.internal.reflect.Native	2024-01-21 21:29:32.069	\N
3	2	3	2024-01-21 21:31:18.387	2024-01-21 21:31:18.434	2024-01-21 21:31:18.589	COMPLETED	COMPLETED		2024-01-21 21:31:18.589	\N
4	2	4	2024-01-28 23:15:15.49	2024-01-28 23:15:15.545	2024-01-28 23:15:15.591	COMPLETED	COMPLETED		2024-01-28 23:15:15.591	\N
5	2	5	2024-01-28 23:15:15.638	2024-01-28 23:15:15.641	2024-01-28 23:15:15.658	COMPLETED	COMPLETED		2024-01-28 23:15:15.658	\N
6	2	6	2024-01-28 23:27:15.529	2024-01-28 23:27:15.575	2024-01-28 23:27:15.608	COMPLETED	COMPLETED		2024-01-28 23:27:15.608	\N
7	2	7	2024-01-28 23:27:15.62	2024-01-28 23:27:15.622	2024-01-28 23:27:15.64	COMPLETED	COMPLETED		2024-01-28 23:27:15.64	\N
8	2	8	2024-01-29 15:06:18.519	2024-01-29 15:06:18.561	2024-01-29 15:06:18.59	COMPLETED	COMPLETED		2024-01-29 15:06:18.59	\N
9	2	9	2024-01-29 15:06:18.603	2024-01-29 15:06:18.606	2024-01-29 15:06:18.622	COMPLETED	COMPLETED		2024-01-29 15:06:18.622	\N
10	2	10	2024-01-29 15:08:04.882	2024-01-29 15:08:04.925	2024-01-29 15:08:26.43	COMPLETED	COMPLETED		2024-01-29 15:08:26.43	\N
11	2	11	2024-01-29 15:08:26.443	2024-01-29 15:08:26.446	2024-01-29 15:08:31.394	COMPLETED	COMPLETED		2024-01-29 15:08:31.394	\N
12	1	12	2024-01-29 17:16:47.607	2024-01-29 17:16:47.651	\N	STARTED	UNKNOWN		2024-01-29 17:16:47.651	\N
\.


--
-- Data for Name: batch_job_execution_context; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_execution_context (job_execution_id, short_context, serialized_context) FROM stdin;
1	{"@class":"java.util.HashMap"}	\N
2	{"@class":"java.util.HashMap"}	\N
3	{"@class":"java.util.HashMap"}	\N
4	{"@class":"java.util.HashMap"}	\N
5	{"@class":"java.util.HashMap"}	\N
6	{"@class":"java.util.HashMap"}	\N
7	{"@class":"java.util.HashMap"}	\N
8	{"@class":"java.util.HashMap"}	\N
9	{"@class":"java.util.HashMap"}	\N
10	{"@class":"java.util.HashMap"}	\N
11	{"@class":"java.util.HashMap"}	\N
12	{"@class":"java.util.HashMap"}	\N
\.


--
-- Data for Name: batch_job_execution_params; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_execution_params (job_execution_id, type_cd, key_name, string_val, date_val, long_val, double_val, identifying) FROM stdin;
1	LONG	run.id		1970-01-01 01:00:00	1	0	Y
2	LONG	run.id		1970-01-01 01:00:00	2	0	Y
3	LONG	run.id		1970-01-01 01:00:00	3	0	Y
4	LONG	run.id		1970-01-01 01:00:00	1	0	Y
5	LONG	run.id		1970-01-01 01:00:00	4	0	Y
6	LONG	run.id		1970-01-01 01:00:00	2	0	Y
7	LONG	run.id		1970-01-01 01:00:00	5	0	Y
8	LONG	run.id		1970-01-01 01:00:00	3	0	Y
9	LONG	run.id		1970-01-01 01:00:00	6	0	Y
10	LONG	run.id		1970-01-01 01:00:00	4	0	Y
11	LONG	run.id		1970-01-01 01:00:00	7	0	Y
12	LONG	run.id		1970-01-01 01:00:00	5	0	Y
\.


--
-- Data for Name: batch_job_instance; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_instance (job_instance_id, version, job_name, job_key) FROM stdin;
1	0	Remuneration Chunk Job ...	853d3449e311f40366811cbefb3d93d7
2	0	Remuneration Chunk Job ...	e070bff4379694c0210a51d9f6c6a564
3	0	Remuneration Chunk Job ...	a3364faf893276dea0caacefbf618db5
4	0	Remuneration Partitioned Job	853d3449e311f40366811cbefb3d93d7
5	0	Remuneration Chunk Job ...	47c0a8118b74165a864b66d37c7b6cf5
6	0	Remuneration Partitioned Job	e070bff4379694c0210a51d9f6c6a564
7	0	Remuneration Chunk Job ...	ce148f5f9c2bf4dc9bd44a7a5f64204c
8	0	Remuneration Partitioned Job	a3364faf893276dea0caacefbf618db5
9	0	Remuneration Chunk Job ...	bd0034040292bc81e6ccac0e25d9a578
10	0	Remuneration Partitioned Job	47c0a8118b74165a864b66d37c7b6cf5
11	0	Remuneration Chunk Job ...	597815c7e4ab1092c1b25130aae725cb
12	0	Remuneration Partitioned Job	ce148f5f9c2bf4dc9bd44a7a5f64204c
\.


--
-- Data for Name: batch_step_execution; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_step_execution (step_execution_id, version, step_name, job_execution_id, start_time, end_time, status, commit_count, read_count, filter_count, write_count, read_skip_count, write_skip_count, process_skip_count, rollback_count, exit_code, exit_message, last_updated) FROM stdin;
1	2	First Chunk Step	1	2024-01-21 21:28:35.095	2024-01-21 21:28:35.175	FAILED	0	0	0	0	0	0	0	0	FAILED	org.springframework.batch.item.ItemStreamException: Failed to initialize the reader\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader.open(AbstractItemCountingItemStreamItemReader.java:153)\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader$$FastClassBySpringCGLIB$$ebb633d0.invoke(<generated>)\r\n\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:783)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.doProceed(DelegatingIntroductionInterceptor.java:137)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.invoke(DelegatingIntroductionInterceptor.java:124)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:698)\r\n\tat org.springframework.batch.item.database.JpaCursorItemReader$$EnhancerBySpringCGLIB$$2797d295.open(<generated>)\r\n\tat org.springframework.batch.item.support.CompositeItemStream.open(CompositeItemStream.java:104)\r\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.open(TaskletStep.java:311)\r\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:205)\r\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:152)\r\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:413)\r\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:136)\r\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:320)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:149)\r\n\tat org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:140)\r\n\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\r\n\tat java.base/jdk.internal.reflect.Nativ	2024-01-21 21:28:35.176
2	2	First Chunk Step	2	2024-01-21 21:29:31.989	2024-01-21 21:29:32.064	FAILED	0	0	0	0	0	0	0	0	FAILED	org.springframework.batch.item.ItemStreamException: Failed to initialize the reader\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader.open(AbstractItemCountingItemStreamItemReader.java:153)\r\n\tat org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader$$FastClassBySpringCGLIB$$ebb633d0.invoke(<generated>)\r\n\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:783)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.doProceed(DelegatingIntroductionInterceptor.java:137)\r\n\tat org.springframework.aop.support.DelegatingIntroductionInterceptor.invoke(DelegatingIntroductionInterceptor.java:124)\r\n\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\r\n\tat org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:753)\r\n\tat org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:698)\r\n\tat org.springframework.batch.item.database.JpaCursorItemReader$$EnhancerBySpringCGLIB$$d1167e1.open(<generated>)\r\n\tat org.springframework.batch.item.support.CompositeItemStream.open(CompositeItemStream.java:104)\r\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.open(TaskletStep.java:311)\r\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:205)\r\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:152)\r\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:413)\r\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:136)\r\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:320)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:149)\r\n\tat org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50)\r\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:140)\r\n\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\r\n\tat java.base/jdk.internal.reflect.Native	2024-01-21 21:29:32.065
11	2	masterStep	11	2024-01-29 15:08:26.452	2024-01-29 15:08:31.391	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-29 15:08:31.391
3	3	First Chunk Step	3	2024-01-21 21:31:18.444	2024-01-21 21:31:18.584	COMPLETED	1	0	0	0	0	0	0	0	COMPLETED		2024-01-21 21:31:18.585
12	1	masterStep	12	2024-01-29 17:16:47.665	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2024-01-29 17:16:47.665
4	2	masterStep	4	2024-01-28 23:15:15.569	2024-01-28 23:15:15.585	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-28 23:15:15.586
5	2	masterStep	5	2024-01-28 23:15:15.649	2024-01-28 23:15:15.653	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-28 23:15:15.653
6	2	masterStep	6	2024-01-28 23:27:15.589	2024-01-28 23:27:15.602	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-28 23:27:15.603
7	2	masterStep	7	2024-01-28 23:27:15.63	2024-01-28 23:27:15.637	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-28 23:27:15.637
8	2	masterStep	8	2024-01-29 15:06:18.573	2024-01-29 15:06:18.585	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-29 15:06:18.586
9	2	masterStep	9	2024-01-29 15:06:18.614	2024-01-29 15:06:18.619	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-29 15:06:18.619
10	2	masterStep	10	2024-01-29 15:08:04.938	2024-01-29 15:08:26.424	COMPLETED	0	0	0	0	0	0	0	0	COMPLETED		2024-01-29 15:08:26.425
\.


--
-- Data for Name: batch_step_execution_context; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_step_execution_context (step_execution_id, short_context, serialized_context) FROM stdin;
1	{"@class":"java.util.HashMap"}	\N
2	{"@class":"java.util.HashMap"}	\N
3	{"@class":"java.util.HashMap","batch.taskletType":"org.springframework.batch.core.step.item.ChunkOrientedTasklet","JpaCursorItemReader.read.count":1,"batch.stepType":"org.springframework.batch.core.step.tasklet.TaskletStep"}	\N
4	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
5	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
6	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
7	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
8	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
9	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
10	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
11	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
12	{"@class":"java.util.HashMap","SimpleStepExecutionSplitter.GRID_SIZE":["java.lang.Long",10],"batch.stepType":"org.springframework.batch.core.partition.support.PartitionStep"}	\N
\.


--
-- Data for Name: clients_transactions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.clients_transactions (id, id_client, num_phone, first_name, last_name, fullname, address, libelle_ville, code_ville, gender, civility, nationality, country_code, type_id, num_id, identity_expirdate, niveau_wallet, dt_sous_wallet, dt_naissance, age, rib, id_client_m2t, rib_compte_interne, id_wallet, dt_entree_relation_m2t, code_oper, type_transaction, mnt, date_validation, code_es) FROM stdin;
1	IDCLIENT1	0612345678	Alice	Smith	Alice Smith	123 Main St	Ville1	C1	F	Ms.	Nationality1	CC1	TypeID1	NumID1	2023-12-31	NiveauWallet1	2023-01-01	1980-01-01	42	RIB1	IDCLIENTM2T1	RIBCPTINT1	IDWALLET1	2023-01-01	OP1	TransacType1	100.00	2023-01-02	ES1
2	IDCLIENT2	0698765432	Bob	Johnson	Bob Johnson	456 Second St	Ville2	C2	M	Mr.	Nationality2	CC2	TypeID2	NumID2	2024-06-15	NiveauWallet2	2023-02-02	1985-02-02	37	RIB2	IDCLIENTM2T2	RIBCPTINT2	IDWALLET2	2023-02-02	OP2	TransacType2	200.00	2023-02-03	ES2
3	IDCLIENT3	0654321789	Charlie	Davis	Charlie Davis	789 Third St	Ville3	C3	M	Dr.	Nationality3	CC3	TypeID3	NumID3	2025-03-20	NiveauWallet3	2023-03-03	1990-03-03	33	RIB3	IDCLIENTM2T3	RIBCPTINT3	IDWALLET3	2023-03-03	OP3	TransacType3	300.00	2023-03-04	ES3
\.


--
-- Data for Name: remuneration; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.remuneration (id, code_es, montant, created_at) FROM stdin;
\.


--
-- Name: batch_job_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.batch_job_execution_seq', 12, true);


--
-- Name: batch_job_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.batch_job_seq', 12, true);


--
-- Name: batch_step_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.batch_step_execution_seq', 12, true);


--
-- Name: clients_transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.clients_transactions_id_seq', 3, true);


--
-- Name: remuneration_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.remuneration_id_seq', 1, false);


--
-- Name: batch_job_execution_context batch_job_execution_context_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution_context
    ADD CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id);


--
-- Name: batch_job_execution batch_job_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution
    ADD CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id);


--
-- Name: batch_job_instance batch_job_instance_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_instance
    ADD CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id);


--
-- Name: batch_step_execution_context batch_step_execution_context_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution_context
    ADD CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id);


--
-- Name: batch_step_execution batch_step_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution
    ADD CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id);


--
-- Name: clients_transactions clients_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clients_transactions
    ADD CONSTRAINT clients_transactions_pkey PRIMARY KEY (id);


--
-- Name: batch_job_instance job_inst_un; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_instance
    ADD CONSTRAINT job_inst_un UNIQUE (job_name, job_key);


--
-- Name: remuneration remuneration_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.remuneration
    ADD CONSTRAINT remuneration_pkey PRIMARY KEY (id);


--
-- Name: batch_job_execution_context job_exec_ctx_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution_context
    ADD CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);


--
-- Name: batch_job_execution_params job_exec_params_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution_params
    ADD CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);


--
-- Name: batch_step_execution job_exec_step_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution
    ADD CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);


--
-- Name: batch_job_execution job_inst_exec_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution
    ADD CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES public.batch_job_instance(job_instance_id);


--
-- Name: batch_step_execution_context step_exec_ctx_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution_context
    ADD CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES public.batch_step_execution(step_execution_id);


--
-- PostgreSQL database dump complete
--

