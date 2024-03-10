package ma.globalperformance.processor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ma.globalperformance.dto.PalierDTO;
import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;

@Component
public class RemunerationItemProcessor implements ItemProcessor<ClientTransaction, Remuneration> {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    private static final String PALIERS_CACHE_NAME = "paliersCache";

    @Override
    public Remuneration process(ClientTransaction item) throws Exception {
        BigDecimal montantRemuneration = null;

        // Cache key based on operation code
        String cacheKey = item.getCodeOper();

        // Check cache for paliers
        Cache paliersCache = cacheManager.getCache(PALIERS_CACHE_NAME);
        List<PalierDTO> paliers = null;
        if ( paliersCache.get(cacheKey) != null)
        	paliers = (List<PalierDTO>) paliersCache.get(cacheKey).get();

        // Handle missing cache data
        if (paliers == null) {
            // Attempt to fetch from WebService
            String url = "http://164.68.125.91:8080/api/v1/paliers/find/oper/" + item.getCodeOper();
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                paliers = response.getBody();
                // Add to cache with expiration time (optional)
                paliersCache.put(cacheKey, paliers);
            } else {
                throw new RuntimeException("Erreur lors de la récupération des paliers");
            }
        }

        // Proceed with calculation if paliers are available
        if (paliers != null) {
            montantRemuneration = calculateRemuneration(item, paliers);
        }

        Remuneration remuneration = new Remuneration();
        remuneration.setCodeEs(item.getCodeEs());
        remuneration.setCreatedAt(new Date());
        remuneration.setMontant(montantRemuneration);
        return remuneration;
    }


    public BigDecimal calculateRemuneration(ClientTransaction transaction, List<PalierDTO> paliers) {
        BigDecimal montantTransaction = transaction.getMnt();
        BigDecimal remuneration = BigDecimal.ZERO;

        for (PalierDTO palier : paliers) {
            if (isTransactionInPalier(montantTransaction, palier)) {
                remuneration = remuneration.add(calculateFrais(montantTransaction, palier));
                break;
            }
        }

        return remuneration;
    }

    private boolean isTransactionInPalier(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal minPalier = BigDecimal.valueOf(palier.getMinPalier());
        BigDecimal maxPalier = palier.getMaxPalier() != null ? BigDecimal.valueOf(palier.getMaxPalier()) : null;
        
        boolean aboveMin = montantTransaction.compareTo(minPalier) >= 0;
        boolean noMaxLimit = (maxPalier == null) || maxPalier.intValue() == -1;
        boolean belowMax = noMaxLimit || montantTransaction.compareTo(maxPalier) <= 0;
        
        return aboveMin && belowMax;
    }

    private BigDecimal calculateFrais(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal frais = BigDecimal.ZERO;

        if (palier.getFraisPourcentage() != null) {
            frais = calculateFraisPourcentage(montantTransaction, palier);
        } else if (palier.getFraisFixe() != null) {
            frais = BigDecimal.valueOf(palier.getFraisFixe());
        }

        return frais;
    }

    private BigDecimal calculateFraisPourcentage(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal frais = montantTransaction.multiply(BigDecimal.valueOf(palier.getFraisPourcentage()));
        BigDecimal minMontant = BigDecimal.valueOf(palier.getMinMontant());
        BigDecimal maxMontant = palier.getMaxMontant() != null ? BigDecimal.valueOf(palier.getMaxMontant()) : BigDecimal.ZERO;

        if (frais.compareTo(minMontant) < 0) {
            return minMontant;
        } else if (maxMontant.intValue() != -1 && frais.compareTo(maxMontant) > 0) {
            return maxMontant;
        } else {
            return frais;
        }
    }
}
