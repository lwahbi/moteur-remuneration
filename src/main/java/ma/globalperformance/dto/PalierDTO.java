package ma.globalperformance.dto;

import java.time.LocalDateTime;

public class PalierDTO {
    private String id;
    private String codeOper;
    private LocalDateTime dateAjout;
    private LocalDateTime dateModif;
    private Double fraisFixe;
    private Double fraisPourcentage;
    private Double minMontant;
    private Double maxMontant;
    private Double minPalier;
    private Double maxPalier;
    private String codeEspace;
    private String typeTransaction;

    public PalierDTO(String id, String codeOper, LocalDateTime dateAjout, LocalDateTime dateModif,
            Double fraisFixe, Double fraisPourcentage, Double minMontant, Double maxMontant,
            Double minPalier, Double maxPalier, String codeEspace, String typeTransaction) {
			this.id = id;
			this.codeOper = codeOper;
			this.dateAjout = dateAjout;
			this.dateModif = dateModif;
			this.fraisFixe = fraisFixe;
			this.fraisPourcentage = fraisPourcentage;
			this.minMontant = minMontant;
			this.maxMontant = maxMontant;
			this.minPalier = minPalier;
			this.maxPalier = maxPalier;
			this.codeEspace = codeEspace;
			this.typeTransaction = typeTransaction;
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCodeOper() {
		return codeOper;
	}

	public void setCodeOper(String codeOper) {
		this.codeOper = codeOper;
	}

	public LocalDateTime getDateAjout() {
		return dateAjout;
	}

	public void setDateAjout(LocalDateTime dateAjout) {
		this.dateAjout = dateAjout;
	}

	public LocalDateTime getDateModif() {
		return dateModif;
	}

	public void setDateModif(LocalDateTime dateModif) {
		this.dateModif = dateModif;
	}

	public Double getFraisFixe() {
		return fraisFixe;
	}

	public void setFraisFixe(Double fraisFixe) {
		this.fraisFixe = fraisFixe;
	}

	public Double getFraisPourcentage() {
		return fraisPourcentage;
	}

	public void setFraisPourcentage(Double fraisPourcentage) {
		this.fraisPourcentage = fraisPourcentage;
	}

	public Double getMinMontant() {
		return minMontant;
	}

	public void setMinMontant(Double minMontant) {
		this.minMontant = minMontant;
	}

	public Double getMaxMontant() {
		return maxMontant;
	}

	public void setMaxMontant(Double maxMontant) {
		this.maxMontant = maxMontant;
	}

	public Double getMinPalier() {
		return minPalier;
	}

	public void setMinPalier(Double minPalier) {
		this.minPalier = minPalier;
	}

	public Double getMaxPalier() {
		return maxPalier;
	}

	public void setMaxPalier(Double maxPalier) {
		this.maxPalier = maxPalier;
	}

	public String getCodeEspace() {
		return codeEspace;
	}

	public void setCodeEspace(String codeEspace) {
		this.codeEspace = codeEspace;
	}

	public String getTypeTransaction() {
		return typeTransaction;
	}

	public void setTypeTransaction(String typeTransaction) {
		this.typeTransaction = typeTransaction;
	}
    
    
}
