package ma.globalperformance.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaliersDto implements Serializable {
    private String id;
    private Double minPalier;
    private Double maxPalier;
    private Double fraisFixe;
    private Double fraisPourcentage;
    private Double minMontant;
    private Double maxMontant;
    private String codeOper;
    private String operLibelle;
    private Boolean traitementUnitaire;
    private String typeMontant;
    private String typeTransaction;
    private String modifiePar;
    private LocalDateTime dateAjout;
    private LocalDateTime dateModif;
}
