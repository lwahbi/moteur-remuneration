package ma.globalperformance.app;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableBatchProcessing
@ComponentScan({"ma.globalperformance.config", "ma.globalperformance.reader", 
	"ma.globalperformance.processor","ma.globalperformance.writer", 
	"ma.globalperformance.listener", "ma.globalperformance.partitioner"})
public class MoteurRemunerationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoteurRemunerationApplication.class, args);
	}

}
