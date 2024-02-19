package ma.globalperformance.listner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class DetailedStepExecutionListener implements StepExecutionListener {

//    @Override
//    public void beforeStep(StepExecution stepExecution) {
//        System.out.println("Début du Step: " + stepExecution.getStepName() + ", Step Execution ID: " + stepExecution.getId() + " à " + stepExecution.getStartTime());
//    }
//
//    @Override
//    public ExitStatus afterStep(StepExecution stepExecution) {
//        System.out.println("Fin du Step: " + stepExecution.getStepName() + ", Step Execution ID: " + stepExecution.getId() + " à " + stepExecution.getEndTime() + ". Statut: " + stepExecution.getStatus());
//        return stepExecution.getExitStatus();
//    }
	
	  private static final Logger logger = LoggerFactory.getLogger(DetailedStepExecutionListener.class);

	    @Override
	    public void beforeStep(StepExecution stepExecution) {
	        stepExecution.getExecutionContext().putLong("stepStartTime", System.currentTimeMillis());
	        logger.info("Remuneration Step {} starting. StepExecutionId: {}", stepExecution.getStepName(), stepExecution.getId());
	    }

	    @Override
	    public ExitStatus afterStep(StepExecution stepExecution) {
	        long startTime = stepExecution.getExecutionContext().getLong("stepStartTime", 0L); // Utilisez 0L pour la valeur par défaut
	        long endTime = System.currentTimeMillis();
	        long duration = endTime - startTime;
	        logger.info("Remuneration Step {} took {} ms", stepExecution.getStepName(), duration);
	        return stepExecution.getExitStatus();
	    }
}

