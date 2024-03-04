package ma.globalperformance.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	@Override
	public Remuneration process(ClientTransaction item) throws Exception {
		BigDecimal montantRemuneration = null;
     // **Récupération des paliers via le Web Service**
     //   List<PalierDTO> paliers = palierServiceClient.findPaliersByOperTypeEs(item.getCodeOper(), item.getTypeTransaction(), item.getCodeEs());
        String url = "http://164.68.125.91:8080/api/v1/paliers/find/oper/0011/type/E/es/002118";

     // Exécuter la requête et récupérer la réponse
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

     // Vérifier le code de status de la réponse
     if (response.getStatusCode() == HttpStatus.OK) {
         List<PalierDTO> paliers = response.getBody();
         montantRemuneration = calculateRemuneration(item, paliers);

     } else {
         // Gérer l'erreur
         System.err.println("Erreur lors de l'appel du WebService : " + response.getStatusCodeValue());
     }
		
		Remuneration remuneration = new Remuneration();
		remuneration.setCodeEs(item.getCodeEs());
		remuneration.setCreatedAt(new Date());
		remuneration.setMontant(montantRemuneration);
		return remuneration;
		
		
	}
	
    private BigDecimal calculateRemuneration(ClientTransaction transaction, List<PalierDTO> paliers) {
        BigDecimal montantTransaction = transaction.getMnt();
        BigDecimal remuneration = BigDecimal.ZERO;

        // Parcourir chaque palier pour calculer la rémunération
        for (PalierDTO palier : paliers) {
            BigDecimal minPalier = BigDecimal.valueOf(palier.getMinPalier());
            BigDecimal maxPalier = palier.getMaxPalier() != null ? BigDecimal.valueOf(palier.getMaxPalier()) : null;
            Double fraisPourcentage = palier.getFraisPourcentage();
            Double fraisFixe = palier.getFraisFixe();

            // Vérifier si le montant de la transaction est dans le palier
            if (montantTransaction.compareTo(minPalier) >= 0 && (maxPalier == null || montantTransaction.compareTo(maxPalier) <= 0)) {
                // Calculer la rémunération en fonction des paramètres du palier
                if (fraisPourcentage != null) {
                    // Pourcentage du montant de la transaction
                    BigDecimal frais = montantTransaction.multiply(BigDecimal.valueOf(fraisPourcentage / 100));
                    // Vérifier si un minimum s'applique
                    if (frais.compareTo(BigDecimal.valueOf(palier.getMinPalier())) < 0) {
                        frais = BigDecimal.valueOf(palier.getMinPalier());
                    }
                    // Ajouter les frais calculés à la rémunération totale
                    remuneration = remuneration.add(frais);
                } else if (fraisFixe != null) {
                    // Montant fixe par transaction
                    remuneration = remuneration.add(BigDecimal.valueOf(fraisFixe));
                }

                // Sortir de la boucle une fois que le palier correspondant est trouvé
                break;
            }
        }
     //   /api/v1/paliers/find/oper/{codeOper}/type/{typeTransaction}/es/{codeEspace}
     //   http://164.68.125.91:8080/api/v1/paliers/find/oper/11/type/E/es/2118
        return remuneration;
    }
	
	/*
	 * public static List<PalierDTO> mockPaliers(int cas) { List<PalierDTO> paliers
	 * = new ArrayList<>();
	 * 
	 * switch (cas) { case 1: // Montant fixe par transaction paliers.add(new
	 * PalierDTO("CODE_OPER", "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(),
	 * false)); break; case 2: // Pourcentage de la commission // Vous devrez
	 * peut-être ajouter des champs 'montantMin' et 'montantMax' à la classe Palier,
	 * si vous n'en disposez pas déjà paliers.add(new PalierDTO("CODE_OPER",
	 * "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(), false)); break; case 3:
	 * // Pourcentage du montant nominal // (identique au cas 2) paliers.add(new
	 * PalierDTO("CODE_OPER", "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(),
	 * false)); break; case 4: // Pourcentage par paliers du montant de la
	 * transaction paliers.add(new PalierDTO("1-50", "TYPE_TRANS", 0.0, new Date(),
	 * "CODE_ES", new Date(), false)); paliers.add(new PalierDTO("51-100",
	 * "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(), false));
	 * paliers.add(new PalierDTO("101-200", "TYPE_TRANS", 0.0, new Date(),
	 * "CODE_ES", new Date(), false)); paliers.add(new PalierDTO(">200",
	 * "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(), false)); break; case 5:
	 * // Pourcentage par paliers du montant de la transaction avec minimum //
	 * (identique au cas 4) paliers.add(new PalierDTO("1-50", "TYPE_TRANS", 0.0, new
	 * Date(), "CODE_ES", new Date(), false)); paliers.add(new PalierDTO("51-100",
	 * "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(), false));
	 * paliers.add(new PalierDTO("101-200", "TYPE_TRANS", 0.0, new Date(),
	 * "CODE_ES", new Date(), false)); paliers.add(new PalierDTO(">200",
	 * "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(), false)); break; case 6:
	 * // Montant fixe par palier paliers.add(new PalierDTO("1-50", "TYPE_TRANS",
	 * 0.0, new Date(), "CODE_ES", new Date(), false)); paliers.add(new
	 * PalierDTO("51-100", "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(),
	 * false)); paliers.add(new PalierDTO("101-200", "TYPE_TRANS", 0.0, new Date(),
	 * "CODE_ES", new Date(), false)); paliers.add(new PalierDTO(">200",
	 * "TYPE_TRANS", 0.0, new Date(), "CODE_ES", new Date(), false)); break;
	 * default: throw new IllegalArgumentException("Cas non valide: " + cas); }
	 * 
	 * return paliers; }
	 * 
	 */
}
