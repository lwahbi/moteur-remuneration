package ma.globalperformance.entity;


import lombok.Data;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "clients_transactions_2")
@Data
public class ClientTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code_oper")
    private String codeOper;
    private String codeService;
    @Column(name = "type_transaction")
    private String typeTransaction;
    private String mnt; // numeric(19, 2) in PostgreSQL is mapped to BigDecimal in Java
    private String montantPrincipal; // numeric(19, 2) in PostgreSQL is mapped to BigDecimal in Java
    private String frais; // numeric(19, 2) in PostgreSQL is mapped to BigDecimal in Java
    private String nombreTrx; // integer in PostgreSQL is mapped to Integer in Java
    private String nombreFacture; // integer in PostgreSQL is mapped to Integer in Java
    @Column(name = "date_validation")
    @Temporal(TemporalType.DATE)
    private Date dateValidation;

    @Column(name = "code_es")
    @Index(name = "code_es_index")
    private String codeEs;

}
