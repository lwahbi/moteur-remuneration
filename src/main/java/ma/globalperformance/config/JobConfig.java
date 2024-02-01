package ma.globalperformance.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;
import ma.globalperformance.listner.RemunerationStepExecutionListener;
import ma.globalperformance.partitioner.CodeEsPartitioner;
import ma.globalperformance.processor.RemunerationItemProcessor;
import ma.globalperformance.repository.ClientTransactionRepository;

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
	private JpaTransactionManager jpaTransactionManager;
	
	@Autowired
    private CodeEsPartitioner codeEsPartitioner;
	
	@Autowired
	private RemunerationItemProcessor remunerationItemProcessor ;

    JobConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Nombre de threads à exécuter en parallèle
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(10);
        executor.initialize();
        return executor;
    }

    @Bean
    public Job partitionedJob() {
        return jobBuilderFactory.get("Remuneration Partitioned Job")
                .incrementer(new RunIdIncrementer())
                .start(masterStep())
                .build();
    }
    
	@Bean
	public Job chunkJob() {
		return jobBuilderFactory.get("Remuneration Chunk Job ...")
				.incrementer(new RunIdIncrementer())
				.start(masterStep())
				.build();
	}
	
	@Bean
    public Step masterStep() {
        return stepBuilderFactory.get("masterStep")
                .partitioner(reumerationChunkStep().getName(), codeEsPartitioner)
                .step(reumerationChunkStep())
                .gridSize(10) // Vous pouvez modifier cela en fonction du nombre de partitions désirées
                .taskExecutor(taskExecutor()) // Utiliser le TaskExecutor pour l'exécution parallèle
                .build();
    }
	
	public Step reumerationChunkStep() {
		return stepBuilderFactory.get("reumerationChunkStep")
				.<ClientTransaction, Remuneration>chunk(3)
				.reader(jpaCursorItemReader())
				.processor(remunerationItemProcessor)
				.writer(jpaItemWriter())
				.listener(new RemunerationStepExecutionListener())
				.transactionManager(jpaTransactionManager)
				.build();
	}
	
	
	@StepScope
	@Bean
	public JpaCursorItemReader<ClientTransaction> jpaCursorItemReader() {
		JpaCursorItemReader<ClientTransaction> jpaCursorItemReader = 
				new JpaCursorItemReader<ClientTransaction>();
		
		jpaCursorItemReader.setEntityManagerFactory(entityManagerFactory);
		
		jpaCursorItemReader.setQueryString("From ClientTransaction");
		
		return jpaCursorItemReader;
	}
	
	public ItemWriter<Remuneration> jpaItemWriter() {
		JpaItemWriter<Remuneration> jpaItemWriter = 
				new JpaItemWriter<Remuneration>();
		
		jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
		
		return jpaItemWriter;
	}
}
