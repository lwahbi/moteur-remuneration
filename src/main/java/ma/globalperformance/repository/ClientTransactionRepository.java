package ma.globalperformance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import ma.globalperformance.entity.ClientTransaction;

@Transactional
public interface ClientTransactionRepository extends JpaRepository<ClientTransaction, Long> {

	@Query(value = "SELECT DISTINCT code_es FROM clients_transactions", nativeQuery = true)
    List<String> findUniqueCodeEs();
}
