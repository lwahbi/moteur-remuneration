package ma.globalperformance.batch;

import ma.globalperformance.entity.Remuneration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Remuneration> remunerations = new ArrayList<>();
        Remuneration remuneration = new Remuneration();
        remuneration.setId(1);
        remuneration.setCodeEs("000015");
        remuneration.setMontant(BigDecimal.valueOf(1000.0));
        remuneration.setCommission(BigDecimal.valueOf(100.0));
        remuneration.setTransactionType("transaction_type");
        remuneration.setCreatedAt(LocalDateTime.now());
        remuneration.setCodeOper("00001");
        remuneration.setCodeService("00001");
        remunerations.add(remuneration);


        ExcelWriter writer = new ExcelWriter();
        writer.writeExcel(remunerations, "remunerations.xlsx");
    }
}
