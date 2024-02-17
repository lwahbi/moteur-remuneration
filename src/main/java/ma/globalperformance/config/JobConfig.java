package ma.globalperformance.config;

import java.util.HashMap;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;
import ma.globalperformance.partitioner.CodeEsPartitioner;
import ma.globalperformance.processor.RemunerationItemProcessor;

@Configuration
public class JobConfig {

	@Autowired
	@Qualifier("datasource")
	private DataSource datasource;

	private final JobBuilderFactory jobBuilderFactory;

	private final StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	@Qualifier("entityManagerFactory")
	private EntityManagerFactory entityManagerFactory;
	
	@Autowired
    private CodeEsPartitioner codeEsPartitioner;
	
	@Autowired
	private RemunerationItemProcessor remunerationItemProcessor ;
	
    @Autowired
    private JpaPagingItemReader<ClientTransaction> reader;

    JobConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6); // Nombre de threads à exécuter en parallèle
        executor.setMaxPoolSize(500);
        executor.setQueueCapacity(300);
        executor.initialize();
        return executor;
    }

	@Bean
	public Job chunkJob() throws Exception {
		return jobBuilderFactory.get("Remuneration Chunk Job ...")
				.incrementer(new RunIdIncrementer())
				.start(masterStep())
				.build();
	}
	
	@Bean
    public Step masterStep() throws Exception {
        return stepBuilderFactory.get("masterStep")
                .partitioner(reumerationChunkStep().getName(), codeEsPartitioner)
                .step(reumerationChunkStep())
                .gridSize(10) // Vous pouvez modifier cela en fonction du nombre de partitions désirées
                .taskExecutor(taskExecutor()) // Utiliser le TaskExecutor pour l'exécution parallèle
                .build();
    }
	
	public Step reumerationChunkStep() throws Exception {
		return stepBuilderFactory.get("reumerationChunkStep")
				.<ClientTransaction, Remuneration>chunk(600)
				.reader(reader)
				.processor(remunerationItemProcessor)
				.writer(jpaItemWriter())
				//.listener(new RemunerationStepExecutionListener())
				//.transactionManager(jpaTransactionManager)
				.build();
	}
	
	
    @Bean(destroyMethod = "") // http://stackoverflow.com/a/23089536
    @StepScope
    public JpaPagingItemReader<ClientTransaction> jpaPagingItemReader (@Value("#{stepExecutionContext[from]}") Integer from,
                                               @Value("#{stepExecutionContext[to]}") Integer to
    ) {
    	JpaPagingItemReader<ClientTransaction> reader = new JpaPagingItemReader<ClientTransaction>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setSaveState(false);

        reader.setQueryString("SELECT clt " +
                              "FROM ClientTransaction AS clt " +
                              "WHERE clt.id BETWEEN :from AND :to");
        reader.setParameterValues(new HashMap<String, Object>() {
            {
                put("from", from.longValue());
                put("to", to.longValue());
            }
        });

        return reader;
    }
	
	public ItemWriter<Remuneration> jpaItemWriter() {
		JpaItemWriter<Remuneration> jpaItemWriter = 
				new JpaItemWriter<Remuneration>();
		
		jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
		
		return jpaItemWriter;
	}
}
