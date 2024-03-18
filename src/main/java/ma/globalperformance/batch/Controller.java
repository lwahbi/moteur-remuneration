package ma.globalperformance.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public void calculate() {
        log.info("Start calculating");
        batcherCalculator.calculateKPIs();
        log.info("End calculating");
    }
}
