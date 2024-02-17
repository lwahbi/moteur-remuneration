package ma.globalperformance.reader;

import org.springframework.batch.item.ItemReader;

import ma.globalperformance.entity.ClientTransaction;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Iterator;
import java.util.List;

public class CustomJpaItemReader implements ItemReader<ClientTransaction> {

    private EntityManager entityManager;
    private Iterator<ClientTransaction> iterator;
    private String codeEs; // Ajoutez cette variable pour stocker le code ES de la partition

    public CustomJpaItemReader(EntityManager entityManager, String codeEs) {
        this.entityManager = entityManager;
        this.codeEs = codeEs;
    }

    @Override
    public ClientTransaction read() throws Exception {
        if (iterator == null || !iterator.hasNext()) {
            // Créez et exécutez votre requête spécifique à votre base de données pour récupérer les données de la partition
            Query query = entityManager.createQuery("SELECT c FROM ClientTransaction ");
            query.setParameter("codeEs", codeEs);
            List<ClientTransaction> results = query.getResultList();
            iterator = results.iterator();
        }

        return iterator.hasNext() ? iterator.next() : null;
    }
}


