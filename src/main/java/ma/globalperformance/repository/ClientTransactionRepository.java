package ma.globalperformance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ma.globalperformance.entity.ClientTransaction;

@Repository
public interface ClientTransactionRepository extends JpaRepository<ClientTransaction, Long> {
	
	@Query("SELECT DISTINCT ct.codeEs FROM ClientTransaction ct")
    List<String> findUniqueCodeEs();

}

