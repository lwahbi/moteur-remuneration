package ma.globalperformance.processor;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;

@Component
public class RemunerationItemProcessor implements ItemProcessor<ClientTransaction, Remuneration> {

	@Override
	public Remuneration process(ClientTransaction item) throws Exception {
		
		Remuneration remuneration = new Remuneration();
		remuneration.setCodeEs(item.getCodeEs());
		remuneration.setCreatedAt(new Date());
		remuneration.setMontant(BigDecimal.valueOf(12.34));
		return remuneration;
		
		
	}

}
