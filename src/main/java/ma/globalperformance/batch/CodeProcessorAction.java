package ma.globalperformance.batch;

import ma.globalperformance.entity.ClientTransaction;

import java.util.List;
import java.util.concurrent.RecursiveAction;

public class CodeProcessorAction extends RecursiveAction {

    private final List<ClientTransaction> codeEs;
    private final int numberOfProcessors;

    private final int low;
    private final int high;

    public CodeProcessorAction(List<ClientTransaction> codeEs, int numberOfProcessors, int low, int high) {
        this.codeEs = codeEs;
        this.numberOfProcessors = numberOfProcessors;
        this.low = low;
        this.high = high;
    }

    @Override
    protected void compute() {
        if (high - low <= numberOfProcessors) {
            for (int i = low; i < high; i++) {
                String code = codeEs.get(i);
                processCodeEs(code, paliers, remunerations, localDateTime);
            }
        } else {
            int mid = low + (high - low) / 2;
            RecursiveAction left = new CodeProcessorAction(codeEs, low, mid);
            RecursiveAction right = new CodeProcessorAction(codeEs, mid, high);
            invokeAll(left, right);
        }
    }
}