package ma.globalperformance.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import ma.globalperformance.dto.PalierDTO;
import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//@Component
@Slf4j
@Service
public class BatcherCalculator {

    private final RestTemplate restTemplate;

    private final JdbcTemplate jdbcTemplate;
    private Integer codeEsSizeT;
    private final Integer REJEU=0;

    public BatcherCalculator(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    // @Scheduled(cron = "* 0/30 * * * *")
    public void calculateKPIs() {
        log.info("Executing periodic task to calculate KPIs");

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
        });

        List<String> codeEs = jdbcTemplate.queryForList("SELECT DISTINCT code_es FROM clients_transactions_2", String.class);
        //insert codeEs in a table remuneration
        codeEs.forEach(s -> jdbcTemplate.update("INSERT INTO remuneration_2 (code_es, code_oper, commission, created_at, montant, trasaction_type) VALUES (?,null,0,null,0,null)", s));

        codeEsSizeT = codeEs.size();
        log.info("size code es trouvé: " + codeEsSizeT);
        List<Remuneration> remunerations = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.now();

        //codeEs.forEach(s -> processCodeEs(s, paliers, remunerations, localDateTime));
        //remunerations.forEach(remuneration -> log.info("Remuneration: " + remuneration));

        LocalDateTime currentTime = LocalDateTime.now();

        long finished = ChronoUnit.MINUTES.between(startTime, currentTime);
        log.info("finished on : " + finished + " Minutes");

        multithreadingProcessor(codeEs, paliers, remunerations, startTime,codeEsSizeT,REJEU);
        remunerations.forEach(remuneration -> log.info("Remuneration: " + remuneration));


        // Batch update remunerations in the database
        jdbcTemplate.batchUpdate("UPDATE remuneration_2 SET montant = ?, commission = ?, trasaction_type = ?, created_at = ?, code_oper = ? WHERE code_es = ?",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Remuneration remuneration = remunerations.get(i);
                        ps.setBigDecimal(1, remuneration.getMontant());
                        ps.setBigDecimal(2, remuneration.getCommission());
                        ps.setString(3, remuneration.getTrasactionType());
                        ps.setDate(4, new java.sql.Date(remuneration.getCreatedAt().getTime()));
                        ps.setString(5, remuneration.getCodeOper());
                        ps.setString(6, remuneration.getCodeEs());
                    }

                    @Override
                    public int getBatchSize() {
                        return remunerations.size();
                    }
                });

        //restTemplate post remunerations
        restTemplate.postForObject("http://164.68.125.91:8080/api/v1/remunerations/saveall", remunerations, List.class);

    }


    private void multithreadingProcessor(List<String> codeEs, List<PalierDTO> paliers, List<Remuneration> remunerations, LocalDateTime startTime, Integer codeEsSize, Integer REJEU ) {
        ExecutorService executorService = Executors.newFixedThreadPool(20); // Increased thread pool size
        Integer codeEsTraite = codeEs.size();
        for (String s : codeEs) {

            Integer finalREJEU = REJEU;
            executorService.submit(() -> processCodeEs(s, paliers, remunerations, startTime, codeEsSize, finalREJEU));
            codeEsTraite--;
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }



        List<String> codeEsRemunerate = remunerations.stream().map(Remuneration::getCodeEs).distinct().toList();
        //delta de deux codeEs et CodeEsRemunerate
        List<String> delta = new ArrayList<>(codeEs);
        delta.removeAll(codeEsRemunerate);
        if (!delta.isEmpty()) {
            REJEU++;
            multithreadingProcessor(delta, paliers, remunerations, startTime, delta.size(),REJEU);
        }

    }

    private void processCodeEs(String s, List<PalierDTO> paliers, List<Remuneration> remunerations, LocalDateTime start, Integer codeEsSize, Integer REJEU) {
        Remuneration remuneration = new Remuneration();
        log.info("code_es: " + s);
        log.info("il reste : " + codeEsSize + " code es à traiter sur " + codeEsSize);
        log.info("REJEU: " + REJEU);
        LocalDateTime currentTime = LocalDateTime.now();

        long timePassed = ChronoUnit.MINUTES.between(start, currentTime);
        log.info("time passed : " + timePassed + " Minutes");

        List<ClientTransaction> transactions = jdbcTemplate.query("SELECT * FROM clients_transactions_2 WHERE code_es = ?", new Object[]{s}, this::mapRow);
        Map<String, List<ClientTransaction>> transactionsParOper = transactions.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeOper));
        transactionsParOper.forEach((codeOper, transactionsList) -> processTransactionsParOper(codeOper, transactionsList, paliers, remuneration, s));
        remunerations.add(remuneration);
    }

    private ClientTransaction mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ClientTransaction transaction = new ClientTransaction();
        transaction.setCodeOper(rs.getString("code_oper"));
        String mnt = rs.getString("mnt").replace(",", ".");
        transaction.setMnt(mnt);
        return transaction;
    }

    private void processTransactionsParOper(String codeOper, List<ClientTransaction> transactionsList, List<PalierDTO> paliers, Remuneration remuneration, String s) {
        //List des paliers opers

      //  PalierDTO palier = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).findFirst().orElse(null);
        List<PalierDTO> paliersOper = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).toList();
        //stop thread if paliersOper is null or empty
        if (paliersOper.isEmpty()) {
            log.info("Pas de palier pour le code oper: " + codeOper);
            return;
        }
        if(paliersOper.size()>1){
            log.info("Plusieurs paliers pour le code oper: " + codeOper);
            //transactionList group by code service
            Map<String, List<ClientTransaction>> transactionsParService = transactionsList.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeService));
            transactionsParService.forEach((codeService, transactionsListService) -> {
                PalierDTO palier = paliersOper.stream().filter(p -> codeService.equals(p.getCodeService())).findFirst().orElse(null);
                if (palier != null && palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
                    transactionsListService.forEach(transaction -> {
                        if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                            remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                            remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getMontantPrincipal()), palier));

                        }
                        if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                            remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                            remuneration.setCommission(new BigDecimal(transaction.getNombreTrx()).multiply(calculateFrais(new BigDecimal(transaction.getFrais()), palier)));

                        }
                        if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                            remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                            remuneration.setCommission(new BigDecimal(transaction.getNombreFacture()).multiply(calculateFrais(new BigDecimal(transaction.getFrais()), palier)));

                        }
                        if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                            remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                            remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getFrais()), palier));

                        }

                       // processTransaction(transaction, palier, remuneration, s);
                       // remuneration.setMontant(montantCalcul);
                        remuneration.setTrasactionType(palier.getDescriptionService());
                        remuneration.setCreatedAt(new java.util.Date());
                        remuneration.setCodeOper(palier.getCodeOper());
                    });
                } else if (palier != null && Boolean.FALSE.equals(palier.getTraitementUnitaire())) {
                    BigDecimal montantCalcul = transactionsListService.stream()
                            .map(transaction -> new BigDecimal(transaction.getMnt()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(montantCalcul);
                        remuneration.setCommission(calculateFrais(montantCalcul, palier));

                    }
                    if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(montantCalcul);
                        remuneration.setCommission(new BigDecimal(transaction.getNombreTrx()).multiply(calculateFrais(new BigDecimal(transaction.getFrais()), palier)));

                    }
                    if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(montantCalcul);

                        remuneration.setCommission(new BigDecimal(transaction.getNombreFacture()).multiply(calculateFrais(new BigDecimal(transaction.getFrais()), palier)));

                    }
                    if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(montantCalcul);
                        BigDecimal frais = transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getFrais()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setCommission(calculateFrais(frais, palier));

                    }

                    remuneration.setCommission(calculateFrais(montantCalcul, palier));
                    remuneration.setMontant(montantCalcul);
                    remuneration.setTrasactionType(palier.getDescriptionService());
                    remuneration.setCreatedAt(new java.util.Date());
                    remuneration.setCodeOper(palier.getNomOper());
                }
            });

        }else {
            log.info("Un seul palier pour le code oper: " + codeOper);
            PalierDTO palier = paliersOper.get(0);
            if (palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
                transactionsList.forEach(transaction -> {

                    if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                        montantCalcul.set(new BigDecimal(transaction.getMontantPrincipal()));
                    }
                    if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                        montantCalcul.set(new BigDecimal(transaction.getMontantPrincipal()));

                    }
                    if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                        montantCalcul.set(transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getFrais()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                    }
                    if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                        montantCalcul.set(transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getFrais()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                    }



                    processTransaction(transaction, palier, remuneration, s);
                    remuneration.setMontant(new BigDecimal(transaction.getMnt()));
                    remuneration.setTrasactionType(palier.getDescriptionService());
                    remuneration.setCreatedAt(new java.util.Date());
                    remuneration.setCodeOper(palier.getCodeOper());
                });
            } else {
                //transactionList somme des montants et frais
                BigDecimal montantCalcul = transactionsList.stream()
                        .map(transaction -> new BigDecimal(transaction.getMnt()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                    montantCalcul = transactionsList.stream()
                            .map(transaction -> new BigDecimal(transaction.getFrais()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                remuneration.setCommission(calculateFrais(montantCalcul, palier));
                remuneration.setMontant(montantCalcul);
                remuneration.setTrasactionType(palier.getDescriptionService());
                remuneration.setCreatedAt(new java.util.Date());
                remuneration.setCodeOper(palier.getNomOper());
            }
        }
        remuneration.setCodeEs(s);

    }

    private void processTransaction(ClientTransaction transaction, PalierDTO palier, Remuneration remuneration, String s) {
        BigDecimal montantCalcul = transaction.getMnt() != null ? new BigDecimal(transaction.getMnt()) : BigDecimal.ZERO;
        String typeMontant = palier.getBaseCalcul();
        if ("Frais".equalsIgnoreCase(typeMontant)) {
            montantCalcul = transaction.getFrais() != null ? new BigDecimal(transaction.getFrais()) : BigDecimal.ZERO;
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
        BigDecimal minMontant = palier.getMinCom() != null ? BigDecimal.valueOf(palier.getMinCom()) : BigDecimal.ZERO;
        BigDecimal maxMontant = palier.getMaxCom() != null ? BigDecimal.valueOf(palier.getMaxCom()) : BigDecimal.ZERO;
        if (frais.compareTo(minMontant) < 0) {
            return minMontant;
        } else if (maxMontant.intValue() != -1 && frais.compareTo(maxMontant) > 0) {
            return maxMontant;
        } else {
            return frais;
        }
    }
}