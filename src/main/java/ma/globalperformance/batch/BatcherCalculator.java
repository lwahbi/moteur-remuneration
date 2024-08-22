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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//@Component
@Slf4j
@Service
public class BatcherCalculator {

    private final RestTemplate restTemplate;

    private final JdbcTemplate jdbcTemplate;
    @Value("${palier.service.url}")
    private String url;
    @Value("${batch.size}")
    private int batchSize;

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
     //   List<String> codeEs = Arrays.asList("000015");


        log.info("size code es trouvé: " + codeEs.size());
        log.info("--------------------------------------------------------------------------------------------------------------------------------------------");

        LocalDateTime startTime = LocalDateTime.now();
        log.info("started calculation on : ");


        List<Remuneration> remunerations = multithreadingProcessorCalcul(codeEs, paliers, startTime);


        LocalDateTime currentTime = LocalDateTime.now();
        long finished = ChronoUnit.SECONDS.between(startTime, currentTime);
        log.info("finished traitement on : " + finished + " Seconds");

        log.info("--------------------------------------------------------------------------------------------------------------------------------------------");
        startTime = LocalDateTime.now();

        log.info("started persist on : ");

        multithreadingProcessorPersist(remunerations);

       // insertRemunerations(remunerations);


        currentTime = LocalDateTime.now();
        finished = ChronoUnit.SECONDS.between(startTime, currentTime);
        log.info("finished presist on : " + finished + " Seconds");

        log.info("--------------------------------------------------------------------------------------------------------------------------------------------");

        // generateCSVFile(remunerations);


        //restTemplate.postForObject(url + "/api/v1/facture/savealles", codeEs, Void.class);



         multithreadingProcessorSendStoredRemunerations();
        //envoyerRemunerations();

        currentTime = LocalDateTime.now();
        finished = ChronoUnit.SECONDS.between(startTime, currentTime);
        log.info("finished send data  on : " + finished + " Seconds");
        // end batch create gp_batch_statistic
        jdbcTemplate.update("UPDATE gp_batch_statistic SET status = ?, updated_at = ? WHERE batch_id = ?", "FINISHED", LocalDateTime.now(), batchId);


    }

    private void generateCSVFile(List<Remuneration> remunerations) {
        // parcours de la liste des remunerations
        // pour chaque remuneration, on ecrit une ligne dans le fichier excel
        // on enregistre le fichier dans le meme repertoire
        // les colonnes du fichier sont : id, code_es, montant, commission, trasnaction_type, created_at, code_oper, code_service
        // le nom du fichier est : remunerations.xlsx
        // le fichier est enregistre dans le repertoire courant

        ExcelWriter writer = new ExcelWriter();
        writer.writeExcel(remunerations, "remunerations.xlsx");
    }

    private void multithreadingProcessorSendStoredRemunerations() {
        //select from all remunerations from  remuneration_2 group by code_es, and send them to the api /api/v1/remunerations/saveall/{codeEs}
        List<Remuneration> remunerations = jdbcTemplate.query("SELECT * FROM remuneration_2", (rs, rowNum) -> {
            Remuneration remuneration = new Remuneration();
            remuneration.setId(rs.getInt("id"));
            remuneration.setCodeEs(rs.getString("code_es"));
            remuneration.setMontant(rs.getBigDecimal("montant"));
            remuneration.setCommission(rs.getBigDecimal("commission"));
            remuneration.setTransactionType(rs.getString("transaction_type"));
            remuneration.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            remuneration.setCodeOper(rs.getString("code_oper"));
            remuneration.setNameOper(rs.getString("name_oper"));
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

    private void multithreadingProcessorPersist(List<Remuneration> allRemunerations) {
        int listSize = allRemunerations.size();
        log.info("Processing persistence of {} remunerations elements ", listSize);
        int batchSize = 100000; // Set your desired batch size
        String sql = "INSERT INTO remuneration_2 (montant, commission, transaction_type, " +
                "created_at, code_oper, code_es, code_service) VALUES (?, ?, ?, ?, ?, ?, ?)";

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < allRemunerations.size(); i += batchSize) {
            int start = i;
            int end = Math.min(i + batchSize, allRemunerations.size());
            final List<Remuneration> batchList = allRemunerations.subList(start, end);

            executorService.submit(() -> {
                jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Remuneration remuneration = batchList.get(i);
                        ps.setBigDecimal(1, remuneration.getMontant());
                        ps.setBigDecimal(2, remuneration.getCommission());
                        ps.setString(3, remuneration.getTransactionType());
                        ps.setTimestamp(4, Timestamp.valueOf(remuneration.getCreatedAt()));
                        ps.setString(5, remuneration.getCodeOper());
                        ps.setString(6, remuneration.getCodeEs());
                        ps.setString(7, remuneration.getCodeService());
                    }

                    @Override
                    public int getBatchSize() {
                        return batchList.size();
                    }
                });
                log.info("Processed batch from index {} to {}. Remaining: {}", start, end - 1, listSize - end);
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Batch processing interrupted", e);
        }
    }

    private void awaitExecutorServiceShutdown(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
            remuneration.setTransactionType(rs.getString("transaction_type"));
            remuneration.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            remuneration.setCodeOper(rs.getString("code_oper"));
            remuneration.setNameOper(rs.getString("name_oper"));
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


    private List<Remuneration> multithreadingProcessorCalcul(List<String> codeEs, List<PalierDTO> paliers, LocalDateTime startTime) {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<Remuneration>>> futures = new ArrayList<>();

        for (String s : codeEs) {
            futures.add(executorService.submit(() -> processCodeEs(s, paliers, startTime)));
        }

        executorService.shutdown();

        List<Remuneration> result = new ArrayList<>();
        for (Future<List<Remuneration>> future : futures) {
            try {
                result.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error in multithreadingProcessorCalcul : " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        return result;
    }

    private void insertRemunerations(List<Remuneration> remunerationsParEs) {
        //group remunerations by code_oper and type_transaction
        // show  all  remunerations int the  list on sytem.out
        //  Map<String, List<Remuneration>> remunerationsParEs = remunerations.stream().collect(Collectors.groupingBy(Remuneration::getCodeEs));
        final AtomicInteger i = new AtomicInteger();

        // remunerationsParEs.forEach((es, remunerationsEs) -> {

        //System.out.println("insertRemunerations iteration ===========>  : ( " + i.getAndIncrement() + " ) - code_es: " + es );
        Map<String, List<Remuneration>> remunerationsParOper = remunerationsParEs.stream().collect(Collectors.groupingBy(Remuneration::getCodeOper));
        remunerationsParOper.forEach((codeOper, remunerationsOper) -> {
            Map<String, List<Remuneration>> remunerationsParTypeTransaction = remunerationsOper.stream().collect(Collectors.groupingBy(Remuneration::getTransactionType));


            remunerationsParTypeTransaction.forEach((type, remunerationsType) -> {

                remunerationsType.forEach(remuneration -> {
                    //  log.info("code_es: " + remuneration.getCodeEs() + " code_oper: " + remuneration.getCodeOper() + " type: " + remuneration.getTransactionType() + " montant: " + remuneration.getMontant() + " commission: " + remuneration.getCommission());

                    jdbcTemplate.batchUpdate("INSERT INTO remuneration_2 (montant, commission, transaction_type, created_at, code_oper, code_es,code_service) VALUES (?, ?, ?, ?, ?, ?,?)", new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            //sum Montant and commission

                            BigDecimal montantCalcul = remuneration.getMontant();
                            ps.setBigDecimal(1, montantCalcul);
                            BigDecimal commissionCalcul = remuneration.getCommission();
                            ps.setBigDecimal(2, commissionCalcul);
                            ps.setString(3, remuneration.getTransactionType());
                            Date date = new Date(System.currentTimeMillis());
                            ps.setDate(4, date);
                            ps.setString(5, remuneration.getCodeOper());
                            ps.setString(6, remuneration.getCodeEs());
                            if (remuneration.getCodeService() != null) {
                                ps.setString(7, remuneration.getCodeService());
                            } else {
                                ps.setString(7, "****");
                            }
                        }

                        @Override
                        public int getBatchSize() {
                            return remunerationsParTypeTransaction.size();
                        }
                    });
                });
            });
        });
        //});
    }

    private List<Remuneration> processCodeEs(String s, List<PalierDTO> paliers, LocalDateTime start) {
        //  log.info("code_es: " + s);
        List<Remuneration> remunerationsResult = new ArrayList<>();

        List<ClientTransaction> transactions = jdbcTemplate.query("SELECT * FROM clients_transactions_2 WHERE code_es = ?", new Object[]{s}, this::mapRow);
        Map<String, List<ClientTransaction>> transactionsParOper = transactions.stream()
                // .filter(clientTransaction -> clientTransaction.getCodeOper().equals("0050"))
                .collect(Collectors.groupingBy(ClientTransaction::getCodeOper));

        transactionsParOper.forEach((codeOper, transactionsList) -> {
            List<Remuneration> remunerations = processTransactionsParOper(codeOper, transactionsList, paliers, s);

            remunerationsResult.addAll(remunerations);
        });
        return remunerationsResult;
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
            // log.info("Plusieurs paliers pour le code oper: " + codeOper);
            //transactionList group by code service
            paliersOper.forEach(palier -> {
                log.info("palier: " + palier.getCodeOper() + " " + palier.getCodeService() + " " + palier.getTraitementUnitaire());
            });

            Map<String, List<ClientTransaction>> transactionsParService = transactionsList.stream().collect(Collectors.groupingBy(ClientTransaction::getCodeService));

            transactionsParService.forEach((codeService, transactionsListService) -> {
           //     PalierDTO palier;
               // if (codeService == null || codeService.isEmpty()) {
             //       palier = paliersOper.get(0);
             //   } else {
                    PalierDTO palier = paliersOper.stream().filter(p -> codeService.equals(p.getCodeService())).findFirst().orElse(null);
             //   }
                if (palier != null && palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
                    PalierDTO finalPalier = palier;
                    transactionsListService.forEach(transaction -> {
                        Remuneration remuneration = new Remuneration();
                        remuneration.setNameOper(finalPalier.getNomOper());
                        if ("Principal".equalsIgnoreCase(finalPalier.getBaseCalcul())) {
                            if (transaction.getMontantPrincipal() != null) {
                                transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                                remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                                remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getMontantPrincipal()), finalPalier));
                            }

                        }
                        if ("Transaction".equalsIgnoreCase(finalPalier.getBaseCalcul())) {
                            if (transaction.getMontantPrincipal() != null) {
                                transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                                remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));

                            }
                            if (transaction.getFrais() != null) {
                                transaction.setFrais(transaction.getFrais().replace(",", "."));
                            }
                            remuneration.setCommission(BigDecimal.valueOf(finalPalier.getFraisFixe()));

                        }
                        if ("Facture".equalsIgnoreCase(finalPalier.getBaseCalcul())) {
                            if (transaction.getMontantPrincipal() != null) {
                                transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                                remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                            }
                            if (transaction.getFrais() != null) {
                                transaction.setFrais(transaction.getFrais().replace(",", "."));
                            }
                            remuneration.setCommission(new BigDecimal(transaction.getNombreFacture()).multiply(BigDecimal.valueOf(finalPalier.getFraisFixe())));
                        }

                        if ("Frais".equalsIgnoreCase(finalPalier.getBaseCalcul())) {
                            if (transaction.getMontantPrincipal() != null) {
                                transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                            }
                            if (transaction.getFrais() != null) {
                                transaction.setFrais(transaction.getFrais().replace(",", "."));
                            }
                            remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                            remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getFrais()), finalPalier));

                        }

                        remuneration.setTransactionType(finalPalier.getDescriptionService());
                        remuneration.setCreatedAt(LocalDateTime.now());
                        remuneration.setCodeOper(finalPalier.getCodeOper());
                        remuneration.setCodeService(codeService);
                        remuneration.setCodeEs(s);
                        remunerations.add(remuneration);
                    });
                } else if (palier != null && Boolean.FALSE.equals(palier.getTraitementUnitaire())) {

                    Remuneration remuneration = new Remuneration();
                    remuneration.setNameOper(palier.getNomOper());
                    if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                        // replace transaction.getMontantPrincipal() with transaction.getMontantPrincipal().replace(",", "."), if transaction.getMontantPrincipal() is not null
                        transactionsList.stream()
                                .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                                .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        remuneration.setCommission(calculateFrais(montantCalcul, palier));

                    }
                    if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                        transactionsList.stream()
                                .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                                .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);

                        remuneration.setMontant(montantCalcul);
                        remuneration.setCommission(BigDecimal.valueOf(palier.getFraisFixe()));

                    }
                    if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                        transactionsList.stream()
                                .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                                .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        Integer nombreFacture = transactionsListService.stream().map(transaction -> Integer.parseInt(transaction.getNombreFacture())).reduce(0, Integer::sum);

                        remuneration.setCommission(new BigDecimal(nombreFacture).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                    }
                    if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                        transactionsList.stream()
                                .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                                .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                        BigDecimal montantCalcul = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setMontant(montantCalcul);
                        transactionsList.stream()
                                .filter(transaction -> transaction != null && transaction.getFrais() != null)
                                .forEach(transaction -> transaction.setFrais(transaction.getFrais().replace(",", ".")));
                        BigDecimal frais = transactionsListService.stream().map(transaction -> new BigDecimal(transaction.getFrais())).reduce(BigDecimal.ZERO, BigDecimal::add);
                        remuneration.setCommission(calculateFrais(frais, palier));

                    }

                    remuneration.setTransactionType(palier.getDescriptionService());
                    remuneration.setCreatedAt(LocalDateTime.now());
                    remuneration.setCodeOper(palier.getCodeOper());
                    remuneration.setCodeService(codeService);
                    remuneration.setCodeEs(s);
                    remunerations.add(remuneration);
                }
            });

        } else {
            // log.info("Un seul palier pour le code oper: " + codeOper);
            PalierDTO palier = paliersOper.get(0);
            if (palier.getTraitementUnitaire() != null && palier.getTraitementUnitaire()) {
                transactionsList.forEach(transaction -> {
                    Remuneration remuneration = new Remuneration();
                    remuneration.setNameOper(palier.getNomOper());
                    if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                        if (transaction.getMontantPrincipal() != null) {
                            transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                        }
                        if (transaction.getFrais() != null) {
                            transaction.setFrais(transaction.getFrais().replace(",", "."));
                        }

                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getMontantPrincipal()), palier));
                    }
                    if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                        if (transaction.getMontantPrincipal() != null) {
                            transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                        }
                        if (transaction.getFrais() != null) {
                            transaction.setFrais(transaction.getFrais().replace(",", "."));
                        }
                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(BigDecimal.valueOf(palier.getFraisFixe()));

                    }
                    if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                        if (transaction.getMontantPrincipal() != null) {
                            transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                        }
                        if (transaction.getFrais() != null) {
                            transaction.setFrais(transaction.getFrais().replace(",", "."));
                        }
                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(new BigDecimal(transaction.getNombreFacture()).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                    }
                    if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                        if (transaction.getMontantPrincipal() != null) {
                            transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", "."));
                        }
                        if (transaction.getFrais() != null) {
                            transaction.setFrais(transaction.getFrais().replace(",", "."));
                        }
                        remuneration.setMontant(new BigDecimal(transaction.getMontantPrincipal()));
                        remuneration.setCommission(calculateFrais(new BigDecimal(transaction.getFrais()), palier));
                    }

                    remuneration.setTransactionType(palier.getDescriptionService());
                    remuneration.setCreatedAt(LocalDateTime.now());
                    remuneration.setCodeOper(palier.getCodeOper());
                    remuneration.setCodeService(palier.getCodeService());
                    remuneration.setCodeEs(s);
                    remunerations.add(remuneration);
                });
            } else {
                //transactionList somme des montants et frais
                Remuneration remuneration = new Remuneration();
                remuneration.setNameOper(palier.getNomOper());
                if ("Principal".equalsIgnoreCase(palier.getBaseCalcul())) {
                    transactionsList.stream()
                            .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                            .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    remuneration.setCommission(calculateFrais(montantCalcul, palier));
                }
                if ("Transaction".equalsIgnoreCase(palier.getBaseCalcul())) {
                    transactionsList.stream()
                            .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                            .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    Integer nombreTrx = transactionsList.stream().map(transaction -> Integer.parseInt(transaction.getNombreTrx())).reduce(0, Integer::sum);
                    remuneration.setCommission(new BigDecimal(nombreTrx).multiply(BigDecimal.valueOf(palier.getFraisFixe())));

                }
                if ("Facture".equalsIgnoreCase(palier.getBaseCalcul())) {
                    transactionsList.stream()
                            .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                            .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);
                    Integer nombreFacture = transactionsList.stream().map(transaction -> Integer.parseInt(transaction.getNombreFacture())).reduce(0, Integer::sum);
                    remuneration.setCommission(new BigDecimal(nombreFacture).multiply(BigDecimal.valueOf(palier.getFraisFixe())));
                }
                if ("Frais".equalsIgnoreCase(palier.getBaseCalcul())) {
                    transactionsList.stream()
                            .filter(transaction -> transaction != null && transaction.getMontantPrincipal() != null)
                            .forEach(transaction -> transaction.setMontantPrincipal(transaction.getMontantPrincipal().replace(",", ".")));
                    BigDecimal montantCalcul = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getMontantPrincipal())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setMontant(montantCalcul);

                    transactionsList.stream()
                            .filter(transaction -> transaction != null && transaction.getFrais() != null)
                            .forEach(transaction -> transaction.setFrais(transaction.getFrais().replace(",", ".")));
                    BigDecimal frais = transactionsList.stream().map(transaction -> new BigDecimal(transaction.getFrais())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    remuneration.setCommission(calculateFrais(frais, palier));

                }

                remuneration.setTransactionType(palier.getDescriptionService());
                remuneration.setCreatedAt(LocalDateTime.now());
                remuneration.setCodeOper(palier.getCodeOper());
                remuneration.setCodeService(palier.getCodeService());
                remuneration.setCodeEs(s);
                remunerations.add(remuneration);
            }
        }
        return remunerations;

    }

    private BigDecimal calculateFrais(BigDecimal montantTransaction, PalierDTO palier) {
        BigDecimal frais = BigDecimal.ZERO;
        if (palier.getFraisPourcentage() != null && palier.getFraisPourcentage() > 0) {
            frais = calculateTransactionFeePercentage(montantTransaction, palier);
        } else if (palier.getFraisFixe() != null && palier.getFraisFixe() > 0) {
            frais = BigDecimal.valueOf(palier.getFraisFixe());
        }
        return frais;
    }


    private BigDecimal calculateTransactionFeePercentage(BigDecimal transactionAmount, PalierDTO tier) {
        // log.info("Calculating transaction fee percentage...");

        BigDecimal fee = calculateFee(transactionAmount, tier);
        BigDecimal minAmount = getAmount(tier.getMinCom());
        BigDecimal maxAmount = getAmount(tier.getMaxCom());

        if (maxAmount.compareTo(BigDecimal.ZERO) > 0 && minAmount.compareTo(BigDecimal.ZERO) > 0) {
            fee = adjustFeeWithinRange(fee, minAmount, maxAmount);
        }
        //log.info("Transaction fee percentage calculated as: " + fee);
        return fee;
    }

    // Extract common logic to get the BigDecimal amount
    private BigDecimal getAmount(Double value) {
        BigDecimal amount = value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
        // log.info("Amount obtained: " + amount);
        return amount;
    }

    private BigDecimal calculateFee(BigDecimal transactionAmount, PalierDTO tier) {
        BigDecimal fee = transactionAmount.multiply(BigDecimal.valueOf(tier.getFraisPourcentage()));
        // log.info("Fee calculated: " + fee);
        return fee;
    }

    // Adjusts the fee within the min and max range
    private BigDecimal adjustFeeWithinRange(BigDecimal fee, BigDecimal minAmount, BigDecimal maxAmount) {
        if (fee.compareTo(minAmount) < 0) {
            //   log.info("Fee is less than minimum amount, adjusting fee to minimum amount.");
            return minAmount;
        } else if (fee.compareTo(maxAmount) > 0) {
            //  log.info("Fee is greater than maximum amount, adjusting fee to maximum amount.");
            return maxAmount;
        }
        //  log.info("Fee is within range, no adjustment needed.");
        return fee;
    }
}