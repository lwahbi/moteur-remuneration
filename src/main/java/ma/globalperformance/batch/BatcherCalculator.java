package ma.globalperformance.batch;

import antlr.TokenStreamRewriteEngine;
import lombok.extern.slf4j.Slf4j;
import ma.globalperformance.dto.PalierDTO;
import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BatcherCalculator {

    private final RestTemplate restTemplate;

    private final JdbcTemplate jdbcTemplate;
    private Integer codeEsSize, codeEsSizeT;

    public BatcherCalculator(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void calculateKPIs() {
        log.info("Executing periodic task to calculate KPIs");
        List<PalierDTO> paliers = restTemplate.getForObject("http://164.68.125.91:8080/api/v1/paliers/findall", List.class);
        List<String> codeEs = jdbcTemplate.queryForList("SELECT DISTINCT code_es FROM clients_transactions", String.class);
        codeEsSize = codeEs.size();
        codeEsSizeT = codeEs.size();
        log.info("size code es trouvé: "+codeEsSizeT);
        List<Remuneration> remunerations = new ArrayList<>();

        codeEs.parallelStream().forEach(s -> processCodeEs(s, paliers, remunerations));

        remunerations.stream().forEach(remuneration -> log.info("Remuneration: " + remuneration));
    }

    private void processCodeEs(String s, List<PalierDTO> paliers, List<Remuneration> remunerations) {
        Remuneration remuneration = new Remuneration();
        log.info("code_es: " + s);
        log.info("il reste : "+codeEsSize+" code es à traiter sur "+codeEsSizeT);

        codeEsSize--;
        List<ClientTransaction> transactions = jdbcTemplate.query("SELECT * FROM clients_transactions WHERE code_es = ?", new Object[]{s}, this::mapRow);
        Map<String, List<ClientTransaction>> transactionsParOper = transactions.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeOper));
        transactionsParOper.forEach((codeOper, transactionsList) -> processTransactions(codeOper, transactionsList, paliers, remuneration, s));
        remunerations.add(remuneration);
    }

    private ClientTransaction mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ClientTransaction transaction = new ClientTransaction();
        transaction.setCodeOper(rs.getString("code_oper"));
        transaction.setMnt(rs.getBigDecimal("mnt"));
        return transaction;
    }

    private void processTransactions(String codeOper, List<ClientTransaction> transactionsList, List<PalierDTO> paliers, Remuneration remuneration, String s) {
        PalierDTO palier = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).findFirst().orElse(null);
        transactionsList.forEach(transaction -> processTransaction(transaction, palier, remuneration, s));
    }

    private void processTransaction(ClientTransaction transaction, PalierDTO palier, Remuneration remuneration, String s) {
        BigDecimal montantTransaction = transaction.getMnt();
        if (palier != null && isTransactionInPalier(montantTransaction, palier)) {
            BigDecimal frais = calculateFrais(montantTransaction, palier);
            log.info("les Frais: " + frais);
            remuneration.setCommission(frais);
        }
        remuneration.setCodeEs(s);
        remuneration.setMontant(montantTransaction);
        remuneration.setTrasactionType(transaction.getTypeTransaction());
        remuneration.setCreatedAt(new java.util.Date());
        remuneration.setCodeOper(transaction.getCodeOper());
    }

    private boolean isTransactionInPalier(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal minPalier = BigDecimal.valueOf(palier.getMinPalier());
        BigDecimal maxPalier = palier.getMaxPalier() != null ? BigDecimal.valueOf(palier.getMaxPalier()) : null;
        boolean aboveMin = montantTransaction.compareTo(minPalier) >= 0;
        boolean noMaxLimit = (maxPalier == null) || maxPalier.intValue() == -1;
        boolean belowMax = noMaxLimit || montantTransaction.compareTo(maxPalier) <= 0;
        return aboveMin && belowMax;
    }

    private BigDecimal calculateFrais(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal frais = BigDecimal.ZERO;
        if (palier.getFraisPourcentage() != null) {
            frais = calculateFraisPourcentage(montantTransaction, palier);
        } else if (palier.getFraisFixe() != null) {
            frais = BigDecimal.valueOf(palier.getFraisFixe());
        }
        return frais;
    }

    private BigDecimal calculateFraisPourcentage(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal frais = montantTransaction.multiply(BigDecimal.valueOf(palier.getFraisPourcentage()));
        BigDecimal minMontant = BigDecimal.valueOf(palier.getMinMontant());
        BigDecimal maxMontant = palier.getMaxMontant() != null ? BigDecimal.valueOf(palier.getMaxMontant()) : BigDecimal.ZERO;
        if (frais.compareTo(minMontant) < 0) {
            return minMontant;
        } else if (maxMontant.intValue() != -1 && frais.compareTo(maxMontant) > 0) {
            return maxMontant;
        } else {
            return frais;
        }
    }
}