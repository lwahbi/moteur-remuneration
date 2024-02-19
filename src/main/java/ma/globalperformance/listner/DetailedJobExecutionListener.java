package ma.globalperformance.listner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class DetailedJobExecutionListener implements JobExecutionListener {
	private static final Logger logger = LoggerFactory.getLogger(DetailedJobExecutionListener.class);
//    @Override
//    public void beforeJob(JobExecution jobExecution) {
//        System.out.println("Début du Job ID: " + jobExecution.getId() + ", Job Name: " + jobExecution.getJobInstance().getJobName() + " à " + jobExecution.getStartTime());
//    }
//
//    @Override
//    public void afterJob(JobExecution jobExecution) {
//        System.out.println("Fin du Job ID: " + jobExecution.getId() + ", Job Name: " + jobExecution.getJobInstance().getJobName() + " à " + jobExecution.getEndTime() + ". Statut: " + jobExecution.getStatus());
//    }

    private Map<String, Long> jobTimes = new ConcurrentHashMap<>();

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getExecutionContext().putLong("jobStartTime", System.currentTimeMillis());
        logger.info("Remuneration Job {} starting. JobExecutionId: {}", jobExecution.getJobInstance().getJobName(), jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long startTime = jobExecution.getExecutionContext().getLong("jobStartTime");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        jobTimes.put(jobExecution.getJobInstance().getJobName(), duration);
        logger.info("Remuneration Job {} took {} ms", jobExecution.getJobInstance().getJobName(), duration);
    }

    public Map<String, Long> getJobTimes() {
        return jobTimes;
    }


}
