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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//@Component
@Slf4j
@Service
public class BatcherCalculator {

    private final RestTemplate restTemplate;

    private final JdbcTemplate jdbcTemplate;
    @Value("${palier.service.url}")
    private String url;

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

        List<PalierDTO> paliers = mapper.convertValue(restTemplate.getForObject(url + "/api/v1/paliers/findall", List.class), new TypeReference<>() {
        });


        assert paliers != null;
        assert !paliers.isEmpty();
        //for each paliers we need to check if the codeOper is not null
        paliers.forEach(palier -> {
            log.info("Palier: " + palier);
            assert palier.getCodeOper() != null;
            assert palier.getTraitementUnitaire() != null;
        });
        //generate UUID for batchId
        UUID batchId = UUID.randomUUID();
        // start batch create gp_batch_statistic
        jdbcTemplate.update("INSERT INTO gp_batch_statistic (batch_id, status, created_at, updated_at, created_by, batch_name) VALUES (?, ?, ?, ?, ?, ?)", batchId, "STARTED", LocalDateTime.now(), LocalDateTime.now(), "SYSTEM", "Calcul batch remuneration");

        //extract month from current date
        LocalDate localDate = LocalDate.now();
        int month = localDate.getMonthValue();

        //strt batch
        jdbcTemplate.update("TRUNCATE TABLE remuneration_2");

        //jdcTemplate Get all the codeEs where month(date_validation) = month
        // String sql = "SELECT DISTINCT code_es FROM clients_transactions_2 WHERE EXTRACT(MONTH FROM TO_TIMESTAMP(date_transaction, 'YYYYMMDDHH24MISS')) = ?";
        // List<String> codeEs = jdbcTemplate.queryForList(sql, new Object[]{month}, String.class);

        // Get all the codeEs
        List<String> codeEs = jdbcTemplate.queryForList("SELECT DISTINCT code_es FROM clients_transactions_2", String.class);
        //List<String> codeEs = Arrays.asList("008066");


        log.info("size code es trouvé: " + codeEs.size());
        LocalDateTime startTime = LocalDateTime.now();

        LocalDateTime currentTime = LocalDateTime.now();

        long finished = ChronoUnit.MINUTES.between(startTime, currentTime);
        log.info("finished on : " + finished + " Minutes");
        multithreadingProcessor(codeEs, paliers, startTime);
        // end batch create gp_batch_statistic
        jdbcTemplate.update("UPDATE gp_batch_statistic SET status = ?, updated_at = ? WHERE batch_id = ?", "FINISHED", LocalDateTime.now(), batchId);

        restTemplate.postForObject(url + "/api/v1/facture/savealles", codeEs, Void.class);


        envoyerRemunerations();


    }

    private void envoyerRemunerations() {
        log.info("Envoi des remunerations");
        //start batch


        //get all remunerations
        List<Remuneration> remunerations = jdbcTemplate.query("SELECT * FROM remuneration_2", (rs, rowNum) -> {
            Remuneration remuneration = new Remuneration();
            remuneration.setId(rs.getInt("id"));
            remuneration.setCodeEs(rs.getString("code_es"));
            remuneration.setMontant(rs.getBigDecimal("montant"));
            remuneration.setCommission(rs.getBigDecimal("commission"));
            remuneration.setTrasactionType(rs.getString("trasaction_type"));
            remuneration.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            remuneration.setCodeOper(rs.getString("code_oper"));
            return remuneration;
        });



        // Create a list to hold all the CompletableFuture objects
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        // Create an instance of RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        //group remunerations by codeEs
        Map<String, List<Remuneration>> remunerationsParEs = remunerations.stream().collect(Collectors.groupingBy(Remuneration::getCodeEs));
        remunerationsParEs.forEach((codeEs, remunerationsList) -> {
            // Create a CompletableFuture
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // Make the POST request /api/v1/remunerations/saveall/{codeEs}
                restTemplate.postForObject(url + "/api/v1/remunerations/saveall/" + codeEs, remunerationsList, Void.class);
            });

            // Add the CompletableFuture to the list
            futures.add(future);
        });

        // Wait for all the CompletableFuture objects to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();


    }


    private List<Remuneration> multithreadingProcessor(List<String> codeEs, List<PalierDTO> paliers, LocalDateTime startTime) {
        ExecutorService executorService = Executors.newFixedThreadPool(20); // Increased thread pool size
        CopyOnWriteArrayList<Remuneration> result = new CopyOnWriteArrayList<>();
        for (String s : codeEs) {
            executorService.submit(() -> {
                List<Remuneration> remunerations = processCodeEs(s, paliers, startTime);
                insertRemunerations(remunerations, s);
            });
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        return result;
    }

    private void insertRemunerations(List<Remuneration> remunerations, String codeEs) {
        //group remunerations by code_oper and type_transaction
        Map<String, List<Remuneration>> remunerationsParOper = remunerations.stream().collect(Collectors.groupingBy(Remuneration::getCodeOper));
        remunerationsParOper.forEach((codeOper, remunerationsList) -> {
            Map<String, List<Remuneration>> remunerationsParTypeTransaction = remunerationsList.stream().collect(Collectors.groupingBy(Remuneration::getTrasactionType));
            remunerationsParTypeTransaction.forEach((s, remunerations1) -> {
                jdbcTemplate.batchUpdate("INSERT INTO remuneration_2 (montant, commission, trasaction_type, created_at, code_oper, code_es) VALUES (?, ?, ?, ?, ?, ?)", new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        //sum Montant and commission

                        BigDecimal montantCalcul = remunerations1.stream().map(remuneration -> remuneration.getMontant()).reduce(BigDecimal.ZERO, BigDecimal::add);
                        ps.setBigDecimal(1, montantCalcul);
                        BigDecimal commissionCalcul = remunerations1.stream().map(remuneration -> remuneration.getCommission()).reduce(BigDecimal.ZERO, BigDecimal::add);
                        ps.setBigDecimal(2, commissionCalcul);
                        ps.setString(3, s);
                        Date date = new Date(System.currentTimeMillis());
                        ps.setDate(4, date);
                        ps.setString(5, codeOper);
                        ps.setString(6, codeEs);
                    }

                    @Override
                    public int getBatchSize() {
                        return remunerationsParTypeTransaction.size();
                    }
                });
            });
        });
    }

    private List<Remuneration> processCodeEs(String s, List<PalierDTO> paliers, LocalDateTime start) {
        log.info("code_es: " + s);
        LocalDateTime currentTime = LocalDateTime.now();
        List<Remuneration> remunerations = new ArrayList<>();

        long timePassed = ChronoUnit.MINUTES.between(start, currentTime);
        log.info("time passed : " + timePassed + " Minutes");

        List<ClientTransaction> transactions = jdbcTemplate.query("SELECT * FROM clients_transactions_2 WHERE code_es = ?", new Object[]{s}, this::mapRow);
        Map<String, List<ClientTransaction>> transactionsParOper = transactions.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeOper));
        transactionsParOper.forEach((codeOper, transactionsList) -> {
            remunerations.addAll(processTransactionsParOper(codeOper, transactionsList, paliers, s));
        });
        return remunerations;
    }

    private ClientTransaction mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ClientTransaction transaction = new ClientTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setCodeOper(rs.getString("code_oper"));
        transaction.setCodeService(rs.getString("code_service"));
        transaction.setTypeTransaction(rs.getString("type_transaction"));
        transaction.setMontantPrincipal(rs.getString("montant_principal"));
        transaction.setFrais(rs.getString("frais"));
        transaction.setNombreTrx(rs.getString("nombre_trx"));
        transaction.setNombreFacture(rs.getString("nombre_facture"));
        transaction.setDateTransaction(rs.getString("date_transaction"));
        transaction.setCodeEs(rs.getString("code_es"));

        return transaction;
    }

    private List<Remuneration> processTransactionsParOper(String codeOper, List<ClientTransaction> transactionsList, List<PalierDTO> paliers, String s) {
        //List des paliers opers
        List<Remuneration> remunerations = new ArrayList<>();

        //  PalierDTO palier = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).findFirst().orElse(null);
        List<PalierDTO> paliersOper = paliers.stream().filter(p -> p.getCodeOper().equals(codeOper)).toList();
        //stop thread if paliersOper is null or empty
        if (paliersOper.isEmpty()) {
            log.info("Pas de palier pour le code oper: " + codeOper);
            return new ArrayList<>();
        }

        //
        if (paliersOper.size() > 1) {
            log.info("Plusieurs paliers pour le code oper: " + codeOper);
            //transactionList group by code service
            Map<String, List<ClientTransaction>> transactionsParService = transactionsList.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeService));

            transactionsParService.forEach((codeService, transactionsListService) -> {
                PalierDTO palier = paliersOper.stream().filter(p -> codeService.equals(p.getCodeService())).findFirst().orElse(null);
                if (palier != null && palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
                    transactionsListService.forEach(transaction -> {
                        Remuneration remuneration = new Remuneration();
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

                        remuneration.setTrasactionType(palier.getDescriptionService());
                        remuneration.setCreatedAt(LocalDateTime.now());
                        remuneration.setCodeOper(palier.getCodeOper());
                        remuneration.setCodeEs(s);
                        remunerations.add(remuneration);
                    });
                } else if (palier != null && Boolean.FALSE.equals(palier.getTraitementUnitaire())) {

                    Remuneration remuneration = new Remuneration();
                    if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        remuneration.setCommission(calculateFrais(montantCalcul, palier));

                    }
                    if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);

                        remuneration.setMontant(montantCalcul);
                        Integer nombreTrx = transactionsListService.stream().map(transaction -> Integer.parseInt(transaction.getNombreTrx())).reduce(0, Integer::sum);
                        remuneration.setCommission(new BigDecimal(nombreTrx).multiply(new BigDecimal(palier.getFraisFixe())));

                    }
                    if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        Integer nombreFacture = transactionsListService.stream().map(transaction -> Integer.parseInt(transaction.getNombreFacture())).reduce(0, Integer::sum);

                        remuneration.setCommission(new BigDecimal(nombreFacture).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                    }
                    if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {

                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        BigDecimal frais = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getFrais())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setCommission(calculateFrais(frais, palier));

                    }

                    remuneration.setTrasactionType(palier.getDescriptionService());
                    remuneration.setCreatedAt(LocalDateTime.now());
                    remuneration.setCodeOper(palier.getNomOper());
                    remuneration.setCodeEs(s);
                    remunerations.add(remuneration);
                }
            });

        } else {
            log.info("Un seul palier pour le code oper: " + codeOper);
            PalierDTO palier = paliersOper.get(0);
            if (palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
                transactionsList.forEach(transaction -> {
                    Remuneration remuneration = new Remuneration();
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
                    remuneration.setCodeEs(s);
                    remunerations.add(remuneration);
                });
            } else {
                //transactionList somme des montants et frais
                Remuneration remuneration = new Remuneration();
                if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    remuneration.setCommission(calculateFrais(montantCalcul, palier));
                }
                if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    Integer nombreTrx = transactionsList.stream().map(transaction -> Integer.parseInt(transaction.getNombreTrx())).reduce(0, Integer::sum);
                    remuneration.setCommission(new BigDecimal(nombreTrx).multiply(BigDecimal.valueOf(palier.getFraisFixe())));

                }
                if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    Integer nombreFacture = transactionsList.stream().map(transaction -> Integer.parseInt(transaction.getNombreFacture())).reduce(0, Integer::sum);
                    remuneration.setCommission(new BigDecimal(nombreFacture).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                }
                if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    BigDecimal frais = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getFrais())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setCommission(calculateFrais(frais, palier));

                }

                remuneration.setTrasactionType(palier.getDescriptionService());
                remuneration.setCreatedAt(LocalDateTime.now());
                remuneration.setCodeOper(palier.getNomOper());
                remuneration.setCodeEs(s);
                remunerations.add(remuneration);
            }
        }
        return remunerations;

    }

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

        if (maxAmount.compareTo(BigDecimal.ZERO) > 0 && minAmount.compareTo(BigDecimal.ZERO) > 0) {
            fee = adjustFeeWithinRange(fee, minAmount, maxAmount);
        }
        log.info("Transaction fee percentage calculated as: " + fee);
        return fee;
    }

    // Extract common logic to get the BigDecimal amount
    private BigDecimal getAmount(Double value) {
        BigDecimal amount = value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
        log.info("Amount obtained: " + amount);
        return amount;
    }

    private BigDecimal calculateFee(BigDecimal transactionAmount, PalierDTO tier) {
        BigDecimal fee = transactionAmount.multiply(BigDecimal.valueOf(tier.getFraisPourcentage()));
        log.info("Fee calculated: " + fee);
        return fee;
    }

    // Adjusts the fee within the min and max range
    private BigDecimal adjustFeeWithinRange(BigDecimal fee, BigDecimal minAmount, BigDecimal maxAmount) {
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