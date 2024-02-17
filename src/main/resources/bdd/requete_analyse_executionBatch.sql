select count(*) from clients_transactions where code_es = 'ES10';
select count(*) from remuneration where code_es = 'ES10';

select count(*) from clients_transactions;

select * from remuneration order by code_Es;

SELECT 
    JOB_INSTANCE.JOB_NAME,
    JOB_EXECUTION.JOB_EXECUTION_ID,
    JOB_EXECUTION.START_TIME,
    STEP_EXECUTION.STEP_NAME,
    STEP_EXECUTION.START_TIME AS STEP_START_TIME,
    STEP_EXECUTION.COMMIT_COUNT,
    STEP_EXECUTION.READ_COUNT,
    STEP_EXECUTION.WRITE_COUNT,
    STEP_EXECUTION.READ_SKIP_COUNT,
    STEP_EXECUTION.WRITE_SKIP_COUNT,
    STEP_EXECUTION.PROCESS_SKIP_COUNT,
    STEP_EXECUTION.STATUS
FROM 
    BATCH_JOB_EXECUTION JOB_EXECUTION
INNER JOIN 
    BATCH_JOB_INSTANCE JOB_INSTANCE ON JOB_INSTANCE.JOB_INSTANCE_ID = JOB_EXECUTION.JOB_INSTANCE_ID
INNER JOIN 
    BATCH_STEP_EXECUTION STEP_EXECUTION ON JOB_EXECUTION.JOB_EXECUTION_ID = STEP_EXECUTION.JOB_EXECUTION_ID
WHERE 
    JOB_EXECUTION.END_TIME IS NULL
ORDER BY 
    JOB_EXECUTION.START_TIME DESC, STEP_EXECUTION.START_TIME DESC;

DELETE FROM public.remuneration;

SELECT code_es, COUNT(*) AS total_transactions
FROM clients_transactions
GROUP BY code_es
ORDER BY total_transactions DESC;



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
-- Name: remuneration id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.remuneration ALTER COLUMN id SET DEFAULT nextval('public.remuneration_id_seq'::regclass);

