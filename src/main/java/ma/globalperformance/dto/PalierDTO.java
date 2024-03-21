package ma.globalperformance.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PalierDTO {
    private String id;
    //Code Oper
    private String codeOper;
    //Nom OPER
    private String nomOper;
    //description operator
    private String descriptionOper;
    //Code Service
    private String codeService;
    //description_service
    private String descriptionService;
    //Traitement unitaire
    private Boolean traitementUnitaire;
    //Base de calcul
    private String baseCalcul;
    //Nbre TRX
    private Integer nbreTrx;
    //Nbre Fact
    private Integer nbreFact;
    //minPalier
    private Double minPalier;
    //maxPalier
    private Double maxPalier;
    //fraisFixe
    private Double fraisFixe;
    //fraisPourcentage
    private Double fraisPourcentage;
    //MinCom
    private Double minCom;
    //MaxCom
    private Double maxCom;
    //Explication
    private String explication;
    //Commentaire
    private String commentaire;
    private String modifiePar;
    private LocalDateTime dateAjout;
    private LocalDateTime dateModif;
}



