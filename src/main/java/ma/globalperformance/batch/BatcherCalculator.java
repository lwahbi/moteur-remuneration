package ma.globalperformance.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import ma.globalperformance.dto.PalierDTO;
import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Component
@Slf4j
@Service
public class BatcherCalculator {

    private final RestTemplate restTemplate;

    private final JdbcTemplate jdbcTemplate;
    private Integer codeEsSize, codeEsSizeT;

    public BatcherCalculator(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    // @Scheduled(cron = "* 0/30 * * * *")
    public void calculateKPIs() {
        log.info("Executing periodic task to calculate KPIs");
        //List<PalierDTO> paliers = (List<PalierDTO>) restTemplate.getForObject("http://164.68.125.91:8080/api/v1/paliers/findall", List.class);

        ObjectMapper mapper = new ObjectMapper(); // or inject it as a dependency
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<PalierDTO> paliers = mapper.convertValue(restTemplate.getForObject("http://164.68.125.91:8080/api/v1/paliers/findall", List.class), new TypeReference<List<PalierDTO>>() {
        });


        assert paliers != null;
        assert !paliers.isEmpty();
        //for each paliers we need to check if the codeOper is not null
        paliers.forEach(palier -> {
            log.info("Palier: " + palier);
            assert palier.getCodeOper() != null;
            assert palier.getTraitementUnitaire() != null;
            assert palier.getTypeTransaction() != null;
            assert palier.getTypeMontant() != null;
        });

        List<String> codeEs = jdbcTemplate.queryForList("SELECT DISTINCT code_es FROM clients_transactions", String.class);
        codeEsSize = codeEs.size();
        codeEsSizeT = codeEs.size();
        log.info("size code es trouvé: " + codeEsSizeT);
        List<Remuneration> remunerations = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.now();
        codeEs.forEach(s -> processCodeEs(s, paliers, remunerations, localDateTime));
        remunerations.forEach(remuneration -> log.info("Remuneration: " + remuneration));
    }

    private void processCodeEs(String s, List<PalierDTO> paliers, List<Remuneration> remunerations, LocalDateTime start) {
        Remuneration remuneration = new Remuneration();
        log.info("code_es: " + s);
        log.info("il reste : " + codeEsSize + " code es à traiter sur " + codeEsSizeT);
        codeEsSize--;
        LocalDateTime currentTime = LocalDateTime.now();

        long timePassed = ChronoUnit.MINUTES.between(start, currentTime);
        log.info("time passed : " + timePassed +" Minutes");

        List<ClientTransaction> transactions = jdbcTemplate.query("SELECT * FROM clients_transactions WHERE code_es = ?", new Object[]{s}, this::mapRow);
        Map<String, List<ClientTransaction>> transactionsParOper = transactions.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeOper));
        transactionsParOper.forEach((codeOper, transactionsList) -> processTransactionsParOper(codeOper, transactionsList, paliers, remuneration, s));
        remunerations.add(remuneration);
    }

    private ClientTransaction mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ClientTransaction transaction = new ClientTransaction();
        transaction.setCodeOper(rs.getString("code_oper"));
        String mnt = rs.getString("mnt").replace(",", ".");
        transaction.setMnt(new BigDecimal(mnt));
        return transaction;
    }

    private void processTransactionsParOper(String codeOper, List<ClientTransaction> transactionsList, List<PalierDTO> paliers, Remuneration remuneration, String s) {
        PalierDTO palier = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).findFirst().orElse(null);
        if (palier == null) {
            log.info("Pas de palier pour le code oper: " + codeOper);
            return;
        }
        if (palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
            transactionsList.forEach(transaction -> {
                processTransaction(transaction, palier, remuneration, s);
                remuneration.setMontant(transaction.getMnt());
                remuneration.setTrasactionType(palier.getTypeTransaction());
                remuneration.setCreatedAt(new java.util.Date());
                remuneration.setCodeOper(palier.getCodeOper());
            });
        } else {
            //transactionList
            BigDecimal montantCalcul = transactionsList.stream().map(ClientTransaction::getMnt).reduce(BigDecimal.ZERO, BigDecimal::add);
            if ("Frais".equalsIgnoreCase(palier.getTypeMontant())) {
                montantCalcul = transactionsList.stream().map(ClientTransaction::getFrais).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            remuneration.setCommission(calculateFrais(montantCalcul, palier));
            remuneration.setMontant(montantCalcul);
            remuneration.setTrasactionType(palier.getTypeTransaction());
            remuneration.setCreatedAt(new java.util.Date());
            remuneration.setCodeOper(palier.getCodeOper());
        }
        remuneration.setCodeEs(s);

    }

    private void processTransaction(ClientTransaction transaction, PalierDTO palier, Remuneration remuneration, String s) {
        BigDecimal montantCalcul = transaction.getMnt();
        String typeMontant = palier.getTypeMontant();
        if ("Frais".equalsIgnoreCase(typeMontant)) {
            montantCalcul = transaction.getFrais();
        }

        if (palier != null && isTransactionInPalier(montantCalcul, palier)) {
            BigDecimal commission = calculateFrais(montantCalcul, palier);
            log.info("les commission: " + commission);
            remuneration.setCommission(commission);
        }

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
        BigDecimal minMontant = palier.getMinMontant() != null ? BigDecimal.valueOf(palier.getMinMontant()) : BigDecimal.ZERO;
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