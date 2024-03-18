package ma.globalperformance.entity;


import lombok.Data;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "clients_transactions")
@Data
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
    private BigDecimal frais; // numeric(19, 2) in PostgreSQL is mapped to BigDecimal in Java
    @Column(name = "date_validation")
    @Temporal(TemporalType.DATE)
    private Date dateValidation;

    @Column(name = "code_es")
    @Index(name = "code_es_index")
    private String codeEs;

}
