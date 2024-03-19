package ma.globalperformance.entity;

import lombok.Data;

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
@Table(name = "remuneration")
@Data
public class  Remuneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String codeEs;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;
    private String codeOper;
    @Column(name = "commission", nullable = false, precision = 19, scale = 2)
    private BigDecimal commission;
    private String trasactionType;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

}
