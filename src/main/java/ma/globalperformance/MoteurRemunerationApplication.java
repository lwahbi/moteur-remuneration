package ma.globalperformance;


import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableBatchProcessing
//@ComponentScan({"ma.globalperformance"})
public class MoteurRemunerationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoteurRemunerationApplication.class, args);
	}

}
