package ma.globalperformance.entity;



import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "clients_transactions")
public class ClientTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_client")
    private String idClient;

    @Column(name = "num_phone")
    private String numPhone;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String fullname;
    private String address;

    @Column(name = "libelle_ville")
    private String libelleVille;

    @Column(name = "code_ville")
    private String codeVille;

    private Character gender; // bpchar(1) in PostgreSQL is mapped to Character in Java
    private String civility;
    private String nationality;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "type_id")
    private String typeId;

    @Column(name = "num_id")
    private String numId;

    @Column(name = "identity_expirdate")
    @Temporal(TemporalType.DATE)
    private Date identityExpirdate;

    @Column(name = "niveau_wallet")
    private String niveauWallet;

    @Column(name = "dt_sous_wallet")
    @Temporal(TemporalType.DATE)
    private Date dtSousWallet;

    @Column(name = "dt_naissance")
    @Temporal(TemporalType.DATE)
    private Date dtNaissance;

    private Integer age;

    private String rib;

    @Column(name = "id_client_m2t")
    private String idClientM2t;

    @Column(name = "rib_compte_interne")
    private String ribCompteInterne;

    @Column(name = "id_wallet")
    private String idWallet;

    @Column(name = "dt_entree_relation_m2t")
    @Temporal(TemporalType.DATE)
    private Date dtEntreeRelationM2t;

    @Column(name = "code_oper")
    private String codeOper;

    @Column(name = "type_transaction")
    private String typeTransaction;

    private BigDecimal mnt; // numeric(19, 2) in PostgreSQL is mapped to BigDecimal in Java

    @Column(name = "date_validation")
    @Temporal(TemporalType.DATE)
    private Date dateValidation;

    @Column(name = "code_es")
    private String codeEs;

    // Getters and Setters
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getIdClient() {
		return idClient;
	}

	public void setIdClient(String idClient) {
		this.idClient = idClient;
	}

	public String getNumPhone() {
		return numPhone;
	}

	public void setNumPhone(String numPhone) {
		this.numPhone = numPhone;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLibelleVille() {
		return libelleVille;
	}

	public void setLibelleVille(String libelleVille) {
		this.libelleVille = libelleVille;
	}

	public String getCodeVille() {
		return codeVille;
	}

	public void setCodeVille(String codeVille) {
		this.codeVille = codeVille;
	}

	public Character getGender() {
		return gender;
	}

	public void setGender(Character gender) {
		this.gender = gender;
	}

	public String getCivility() {
		return civility;
	}

	public void setCivility(String civility) {
		this.civility = civility;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public String getNumId() {
		return numId;
	}

	public void setNumId(String numId) {
		this.numId = numId;
	}

	public Date getIdentityExpirdate() {
		return identityExpirdate;
	}

	public void setIdentityExpirdate(Date identityExpirdate) {
		this.identityExpirdate = identityExpirdate;
	}

	public String getNiveauWallet() {
		return niveauWallet;
	}

	public void setNiveauWallet(String niveauWallet) {
		this.niveauWallet = niveauWallet;
	}

	public Date getDtSousWallet() {
		return dtSousWallet;
	}

	public void setDtSousWallet(Date dtSousWallet) {
		this.dtSousWallet = dtSousWallet;
	}

	public Date getDtNaissance() {
		return dtNaissance;
	}

	public void setDtNaissance(Date dtNaissance) {
		this.dtNaissance = dtNaissance;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getRib() {
		return rib;
	}

	public void setRib(String rib) {
		this.rib = rib;
	}

	public String getIdClientM2t() {
		return idClientM2t;
	}

	public void setIdClientM2t(String idClientM2t) {
		this.idClientM2t = idClientM2t;
	}

	public String getRibCompteInterne() {
		return ribCompteInterne;
	}

	public void setRibCompteInterne(String ribCompteInterne) {
		this.ribCompteInterne = ribCompteInterne;
	}

	public String getIdWallet() {
		return idWallet;
	}

	public void setIdWallet(String idWallet) {
		this.idWallet = idWallet;
	}

	public Date getDtEntreeRelationM2t() {
		return dtEntreeRelationM2t;
	}

	public void setDtEntreeRelationM2t(Date dtEntreeRelationM2t) {
		this.dtEntreeRelationM2t = dtEntreeRelationM2t;
	}

	public String getCodeOper() {
		return codeOper;
	}

	public void setCodeOper(String codeOper) {
		this.codeOper = codeOper;
	}

	public String getTypeTransaction() {
		return typeTransaction;
	}

	public void setTypeTransaction(String typeTransaction) {
		this.typeTransaction = typeTransaction;
	}

	public BigDecimal getMnt() {
		return mnt;
	}

	public void setMnt(BigDecimal mnt) {
		this.mnt = mnt;
	}

	public Date getDateValidation() {
		return dateValidation;
	}

	public void setDateValidation(Date dateValidation) {
		this.dateValidation = dateValidation;
	}

	public String getCodeEs() {
		return codeEs;
	}

	public void setCodeEs(String codeEs) {
		this.codeEs = codeEs;
	}


}
