package ma.globalperformance.listner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.ExitStatus;

public class RemunerationStepExecutionListener implements StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(RemunerationStepExecutionListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String codeEs = stepExecution.getExecutionContext().getString("codeEs", "Inconnu");
        logger.info("Début du Step pour codeEs: {}, Nom: {}, ExecutionId: {}", codeEs, stepExecution.getStepName(), stepExecution.getId());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String codeEs = stepExecution.getExecutionContext().getString("codeEs", "Inconnu");
        logger.info("Fin du Step pour codeEs: {}, Nom: {}, ExecutionId: {}, Statut: {}", codeEs, stepExecution.getStepName(), stepExecution.getId(), stepExecution.getExitStatus());
        return stepExecution.getExitStatus();
    }
}

