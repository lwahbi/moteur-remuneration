package ma.globalperformance.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RequestMapping("/batch")
@RestController
@Slf4j
@CrossOrigin("*")

public class Controller {

    private final BatcherCalculator batcherCalculator;

    public Controller(BatcherCalculator batcherCalculator) {
        this.batcherCalculator = batcherCalculator;
    }

    @RequestMapping("/calculate")
    public String calculate() {
        StringBuilder message = new StringBuilder();
        LocalDateTime startTime = LocalDateTime.now();
        message.append("Start calculating at " + startTime).append("\n");
        ;
        log.info("Start calculating");
        batcherCalculator.calculateKPIs();
        log.info("End calculating");
        LocalDateTime currentTime = LocalDateTime.now();
        long finished = ChronoUnit.SECONDS.between(startTime, currentTime);


        message.append("End calculating at " + System.currentTimeMillis()).append("\n");

        message.append("finished calcul on : " + finished + " Seconds");
        return message.toString();
    }
}
