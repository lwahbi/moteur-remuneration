package ma.globalperformance.partitioner;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
public class CodeEsPartitioner implements Partitioner {

    private final List<String> codeEsList; // Liste des valeurs uniques de codeEs

    public CodeEsPartitioner(List<String> codeEsList) {
        this.codeEsList = codeEsList;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        int number = 0;
        for (String codeEs : codeEsList) {
            ExecutionContext value = new ExecutionContext();
            value.putString("codeEs", codeEs);
            result.put("partition" + number, value);
            number++;
        }

        return result;
    }
}
