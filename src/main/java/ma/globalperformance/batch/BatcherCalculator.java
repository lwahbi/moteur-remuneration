package ma.globalperformance.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import ma.globalperformance.dto.PalierDTO;
import ma.globalperformance.entity.ClientTransaction;
import ma.globalperformance.entity.Remuneration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//@Component
@Slf4j
@Service
public class BatcherCalculator {

    private final RestTemplate restTemplate;

    private final JdbcTemplate jdbcTemplate;
    private Integer codeEsSizeT;
    private final Integer REJEU = 0;
    @Value("${palier.service.url}")
    private String url ;

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

        List<PalierDTO> paliers = mapper.convertValue(restTemplate.getForObject(url+"/api/v1/paliers/findall", List.class), new TypeReference<List<PalierDTO>>() {
        });


        assert paliers != null;
        assert !paliers.isEmpty();
        //for each paliers we need to check if the codeOper is not null
        paliers.forEach(palier -> {
            log.info("Palier: " + palier);
            assert palier.getCodeOper() != null;
            assert palier.getTraitementUnitaire() != null;
        });

        //List<String> codeEs = jdbcTemplate.queryForList("SELECT DISTINCT code_es FROM clients_transactions_2", String.class);
        List<String> codeEs = Arrays.asList("008066","000509");
        //insert codeEs in a table remuneration
        //codeEs.forEach(s -> jdbcTemplate.update("INSERT INTO remuneration_2 (code_es, code_oper, commission, created_at, montant, trasaction_type) VALUES (?,null,0,null,0,null)", s));

        jdbcTemplate.batchUpdate(
                "INSERT INTO remuneration_2 (code_es, code_oper, commission, created_at, montant, trasaction_type) VALUES (?,null,0,null,0,null)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, codeEs.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return codeEs.size();
                    }
                }
        );


        codeEsSizeT = codeEs.size();
        log.info("size code es trouvé: " + codeEsSizeT);
        List<Remuneration> remunerations = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.now();

        //codeEs.forEach(s -> processCodeEs(s, paliers, remunerations, localDateTime));
        //remunerations.forEach(remuneration -> log.info("Remuneration: " + remuneration));

        LocalDateTime currentTime = LocalDateTime.now();

        long finished = ChronoUnit.MINUTES.between(startTime, currentTime);
        log.info("finished on : " + finished + " Minutes");

        multithreadingProcessor(codeEs, paliers, remunerations, startTime, codeEsSizeT, REJEU);
       // remunerations.forEach(remuneration -> log.info("Remuneration: " + remuneration));


        // Batch update remunerations in the database
        jdbcTemplate.batchUpdate("UPDATE remuneration_2 SET montant = ?, commission = ?, trasaction_type = ?, created_at = ?, code_oper = ? WHERE code_es = ?",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Remuneration remuneration = remunerations.get(i);
                        ps.setBigDecimal(1, remuneration.getMontant()!=null?remuneration.getMontant():BigDecimal.ZERO);
                        ps.setBigDecimal(2, remuneration.getCommission()!=null?remuneration.getCommission():BigDecimal.ZERO);
                        ps.setString(3, remuneration.getTrasactionType());
                        //Date date = Date.now();
                        Date date = new Date(System.currentTimeMillis());
                        ps.setDate(4, date);
                        ps.setString(5, remuneration.getCodeOper());
                        ps.setString(6, remuneration.getCodeEs());
                    }

                    @Override
                    public int getBatchSize() {
                        return remunerations.size();
                    }
                });

        //restTemplate post remunerations
        // restTemplate.postForObject("http://164.68.125.91:8080/api/v1/remunerations/saveall", remunerations, List.class);

        // Create a list to hold all the CompletableFuture objects
        List<CompletableFuture<Void>> futures = new ArrayList<>();

// Create an instance of RestTemplate
        RestTemplate restTemplate = new RestTemplate();

// Loop through each remuneration
        for (Remuneration remuneration : remunerations) {
            // Create a CompletableFuture
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // Make the POST request
                restTemplate.postForObject(url+"/api/v1/remunerations/save", remuneration, Void.class);
            });

            // Add the CompletableFuture to the list
            futures.add(future);
        }

// Wait for all the CompletableFuture objects to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();


    }


    private void multithreadingProcessor(List<String> codeEs, List<PalierDTO> paliers, List<Remuneration> remunerations, LocalDateTime startTime, Integer codeEsSize, Integer REJEU) {
        ExecutorService executorService = Executors.newFixedThreadPool(20); // Increased thread pool size
        int codeEsTraite = codeEs.size();
        for (String s : codeEs) {

            Integer finalCodeEsTraite = codeEsTraite;
            executorService.submit(() -> processCodeEs(s, paliers, remunerations, startTime, finalCodeEsTraite));
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
            if (REJEU > 3) {
                //Rejeu atteint le max
                log.info("REJEU atteint le max : " + REJEU);
                return;
            }

            multithreadingProcessor(delta, paliers, remunerations, startTime, delta.size(), REJEU);
        }

    }

    private void processCodeEs(String s, List<PalierDTO> paliers, List<Remuneration> remunerations, LocalDateTime start, Integer codeEsSize) {
        Remuneration remuneration = new Remuneration();
        log.info("code_es: " + s);
        log.info("il reste : " + codeEsSize + " code es à traiter sur " + codeEsSizeT);
        log.info("REJEU: " + REJEU);
        LocalDateTime currentTime = LocalDateTime.now();

        long timePassed = ChronoUnit.MINUTES.between(start, currentTime);
        log.info("time passed : " + timePassed + " Minutes");

        List<ClientTransaction> transactions = jdbcTemplate.query("SELECT * FROM clients_transactions_2 WHERE code_es = ?", new Object[]{s}, this::mapRow);
        Map<String, List<ClientTransaction>> transactionsParOper = transactions.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeOper));
        transactionsParOper.forEach((codeOper, transactionsList) -> {
            processTransactionsParOper(codeOper, remunerations, transactionsList, paliers, s);
        });
    }

    private ClientTransaction mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ClientTransaction transaction = new ClientTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setCodeOper(rs.getString("code_oper"));
        transaction.setCodeService(rs.getString("code_service"));
        transaction.setTypeTransaction(rs.getString("type_transaction"));
        transaction.setMnt(rs.getString("mnt"));
        transaction.setMontantPrincipal(rs.getString("montant_principal"));
        transaction.setFrais(rs.getString("frais"));
        transaction.setNombreTrx(rs.getString("nombre_trx"));
        transaction.setNombreFacture(rs.getString("nombre_facture"));
        transaction.setDateValidation(rs.getDate("date_validation"));
        transaction.setCodeEs(rs.getString("code_es"));

        return transaction;
    }

    private void processTransactionsParOper(String codeOper, List<Remuneration> remunerations, List<ClientTransaction> transactionsList, List<PalierDTO> paliers, String s) {
        //List des paliers opers
        Remuneration remuneration = new Remuneration();

        //  PalierDTO palier = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).findFirst().orElse(null);
        List<PalierDTO> paliersOper = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).toList();
        //stop thread if paliersOper is null or empty
        if (paliersOper.isEmpty()) {
            log.info("Pas de palier pour le code oper: " + codeOper);
            return;
        }
        if (paliersOper.size() > 1) {
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
                        remuneration.setCreatedAt(LocalDateTime.now());
                        remuneration.setCodeOper(palier.getCodeOper());
                    });
                } else if (palier != null && Boolean.FALSE.equals(palier.getTraitementUnitaire())) {


                    if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                        BigDecimal montantCalcul = transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getMnt()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        remuneration.setCommission(calculateFrais(montantCalcul, palier));

                    }
                    if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                        BigDecimal montantCalcul = transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getMontantPrincipal()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        remuneration.setMontant(montantCalcul);
                        Integer nombreTrx = transactionsListService.stream()
                                .map(transaction -> Integer.parseInt(transaction.getNombreTrx()))
                                .reduce(0, Integer::sum);
                        remuneration.setCommission(new BigDecimal(nombreTrx).multiply(new BigDecimal(palier.getFraisFixe())));

                    }
                    if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                        BigDecimal montantCalcul = transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getMontantPrincipal()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        Integer nombreFacture = transactionsListService.stream()
                                .map(transaction -> Integer.parseInt(transaction.getNombreFacture()))
                                .reduce(0, Integer::sum);

                        remuneration.setCommission(new BigDecimal(nombreFacture).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                    }
                    if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {

                        BigDecimal montantCalcul = transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getMontantPrincipal()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        BigDecimal frais = transactionsListService.stream()
                                .map(transaction -> new BigDecimal(transaction.getFrais()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setCommission(calculateFrais(frais, palier));

                    }

                    remuneration.setTrasactionType(palier.getDescriptionService());
                    remuneration.setCreatedAt(LocalDateTime.now());
                    remuneration.setCodeOper(palier.getNomOper());
                }
            });

        } else {
            log.info("Un seul palier pour le code oper: " + codeOper);
            PalierDTO palier = paliersOper.get(0);
            if (palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
                transactionsList.forEach(transaction -> {

                    if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getMontantPrincipal()), palier));
                    }
                    if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(new BigDecimal(transaction.getNombreTrx()).multiply(BigDecimal.valueOf(palier.getFraisFixe())));

                    }
                    if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(new BigDecimal(transaction.getNombreFacture()).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                    }
                    if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getFrais()), palier));
                    }

                    remuneration.setTrasactionType(palier.getDescriptionService());
                    remuneration.setCreatedAt(LocalDateTime.now());
                    remuneration.setCodeOper(palier.getCodeOper());
                });
            } else {
                //transactionList somme des montants et frais

                if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream()
                            .map(transaction -> new BigDecimal(transaction.getMontantPrincipal()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    remuneration.setCommission(calculateFrais(montantCalcul, palier));
                }
                if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream()
                            .map(transaction -> new BigDecimal(transaction.getMontantPrincipal()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    Integer nombreTrx = transactionsList.stream()
                            .map(transaction -> Integer.parseInt(transaction.getNombreTrx()))
                            .reduce(0, Integer::sum);
                    remuneration.setCommission(new BigDecimal(nombreTrx).multiply(BigDecimal.valueOf(palier.getFraisFixe())));

                }
                if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream()
                            .map(transaction -> new BigDecimal(transaction.getMontantPrincipal()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    Integer nombreFacture = transactionsList.stream()
                            .map(transaction -> Integer.parseInt(transaction.getNombreFacture()))
                            .reduce(0, Integer::sum);
                    remuneration.setCommission(new BigDecimal(nombreFacture).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                }
                if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream()
                            .map(transaction -> new BigDecimal(transaction.getMontantPrincipal()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    BigDecimal frais = transactionsList.stream()
                            .map(transaction -> new BigDecimal(transaction.getFrais()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setCommission(calculateFrais(frais, palier));

                }


                remuneration.setTrasactionType(palier.getDescriptionService());
                remuneration.setCreatedAt(LocalDateTime.now());
                remuneration.setCodeOper(palier.getNomOper());
            }
        }
        remuneration.setCodeEs(s);
        remunerations.add(remuneration);

    }

    /*
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

     */
/*
    private boolean isTransactionInPalier(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal minPalier = BigDecimal.valueOf(palier.getMinPalier());
        BigDecimal maxPalier = palier.getMaxPalier() != null ? BigDecimal.valueOf(palier.getMaxPalier()) : null;
        boolean aboveMin = montantTransaction.compareTo(minPalier) >= 0;
        boolean noMaxLimit = (maxPalier == null) || maxPalier.intValue() == -1;
        boolean belowMax = noMaxLimit || montantTransaction.compareTo(maxPalier) <= 0;
        return aboveMin && belowMax;
    }
*/
    private BigDecimal calculateFrais(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal frais = BigDecimal.ZERO;
        if (palier.getFraisPourcentage() != null) {
            frais = calculateTransactionFeePercentage(montantTransaction, palier);
        } else if (palier.getFraisFixe() != null) {
            frais = BigDecimal.valueOf(palier.getFraisFixe());
        }
        return frais;
    }


    private BigDecimal calculateTransactionFeePercentage(BigDecimal transactionAmount, PalierDTO tier) {
        log.info("Calculating transaction fee percentage...");

        BigDecimal fee = calculateFee(transactionAmount, tier);
        BigDecimal minAmount = getAmount(tier.getMinCom());
        BigDecimal maxAmount = getAmount(tier.getMaxCom());

        if(maxAmount.compareTo(BigDecimal.ZERO) > 0 && minAmount.compareTo(BigDecimal.ZERO) > 0) {
            fee = adjustFeeWithinRange(fee, minAmount, maxAmount);
        }
        log.info("Transaction fee percentage calculated as: " + fee);
        return fee;
    }

    // Extract common logic to get the BigDecimal amount
    private BigDecimal getAmount(Double value){
        BigDecimal amount = value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
        log.info("Amount obtained: " + amount);
        return amount;
    }

    private BigDecimal calculateFee(BigDecimal transactionAmount, PalierDTO tier){
        BigDecimal fee = transactionAmount.multiply(BigDecimal.valueOf(tier.getFraisPourcentage()));
        log.info("Fee calculated: " + fee);
        return fee;
    }

    // Adjusts the fee within the min and max range
    private BigDecimal adjustFeeWithinRange(BigDecimal fee, BigDecimal minAmount, BigDecimal maxAmount){
        if (fee.compareTo(minAmount) < 0) {
            log.info("Fee is less than minimum amount, adjusting fee to minimum amount.");
            return minAmount;
        } else if (fee.compareTo(maxAmount) > 0) {
            log.info("Fee is greater than maximum amount, adjusting fee to maximum amount.");
            return maxAmount;
        }
        log.info("Fee is within range, no adjustment needed.");
        return fee;
    }
}