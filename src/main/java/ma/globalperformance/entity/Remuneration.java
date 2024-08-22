package ma.globalperformance.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "remuneration_2")
@Data
public class Remuneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String codeEs;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;
    private String codeOper;
    private String nameOper;
    private String codeService;
    @Column(name = "commission", nullable = false, precision = 19, scale = 2)
    private BigDecimal commission;
    private String transactionType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
