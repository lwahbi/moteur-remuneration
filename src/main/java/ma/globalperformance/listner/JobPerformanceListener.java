package ma.globalperformance.listner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobPerformanceListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(JobPerformanceListener.class);

//    @Override
//    public void beforeJob(JobExecution jobExecution) {
//        logger.info("Job start - ID: {} at {}", jobExecution.getJobId(), jobExecution.getStartTime());
//    }
//
//    @Override
//    public void afterJob(JobExecution jobExecution) {
//        logger.info("Job end - ID: {} at {}", jobExecution.getJobId(), jobExecution.getEndTime());
//        logger.info("Job duration for ID {}: {} ms", jobExecution.getJobId(), (jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime()));
//        logger.info("Job Status for ID {}: {}", jobExecution.getJobId(), jobExecution.getStatus());
//    }
    
    private Map<String, Long> jobTimes = new ConcurrentHashMap<>();

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getExecutionContext().putLong("jobStartTime", System.currentTimeMillis());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long startTime = jobExecution.getExecutionContext().getLong("jobStartTime");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        jobTimes.put(jobExecution.getJobInstance().getJobName(), duration);
        logger.info("Job {} took {} ms", jobExecution.getJobInstance().getJobName(), duration);
    }

    public Map<String, Long> getJobTimes() {
        return jobTimes;
    }
}
