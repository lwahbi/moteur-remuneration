package ma.globalperformance.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;

@Component
public class RemunerationItemProcessor implements ItemProcessor<ClientTransaction, Remuneration> {

	@Override
	public Remuneration process(ClientTransaction item) throws Exception {
		
		Remuneration remuneration = new Remuneration();
		remuneration.setCodeEs(null);
		return remuneration;
		
		
	}

}
