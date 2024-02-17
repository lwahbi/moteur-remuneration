package ma.globalperformance.partitioner;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ma.globalperformance.model.CodeEsService;
import ma.globalperformance.repository.ClientTransactionRepository;
import static java.util.stream.Collectors.toMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

@Component
public class CodeEsPartitioner implements Partitioner {

    private List<String> codeEsList;
    
    @Autowired
    private ClientTransactionRepository clientTransactionRepository;
//    @Autowired
//    private JobParameters jobParameters;
//
    @Autowired
    public CodeEsPartitioner(CodeEsService codeEsService) {
        this.codeEsList = codeEsService.getCodeEsList();
    }

	/*
	 * @Override public Map<String, ExecutionContext> partition(int gridSize) {
	 * Map<String, ExecutionContext> result = new HashMap<>(); //
	 * JobParametersBuilder jobParametersBuilder = new
	 * JobParametersBuilder(jobParameters); //
	 * jobParametersBuilder.addString("codeEs", codeEs); // Ajoutez la valeur de
	 * codeEs aux paramètres de job
	 * 
	 * int maxPartitions = Math.min(codeEsList.size(), gridSize);
	 * 
	 * for (int i = 0; i < maxPartitions; i++) { ExecutionContext value = new
	 * ExecutionContext(); value.putString("codeEs", codeEsList.get(i));
	 * result.put("partition" + i, value); }
	 * 
	 * return result; }
	 */
    
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long totalItems = clientTransactionRepository.count();
        System.out.println("\nTotal items: " + totalItems);

        int range = (int) Math.ceil((double) totalItems / gridSize); // Calculer la plage en arrondissant vers le haut
        if (range < 100) {
            throw new IllegalArgumentException();
        }

        return IntStream.range(1, gridSize + 1).boxed()
                .map(index -> {
                    ExecutionContext context = new ExecutionContext();
                    context.putString("name", "partition-" + index);
                    int from = (index - 1) * range + 1;
                    int to = index * range;
                    if (to > totalItems) {
                        to = (int) totalItems;
                    }
                    context.putInt("from", from);
                    context.putInt("to", to);
                    return context;
                })
                .map(context -> {
                    System.out.format("\nCREATED PARTITION: '%s', RANGE FROM %d, TO %d\n",
                            context.getString("name"),
                            context.getInt("from"),
                            context.getInt("to"));
                    return context;
                })
                .collect(toMap(context -> context.getString("name"), Function.identity()));
    }
}
