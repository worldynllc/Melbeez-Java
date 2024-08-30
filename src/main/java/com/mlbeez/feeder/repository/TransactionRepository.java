package com.mlbeez.feeder.repository;
import com.mlbeez.feeder.model.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface TransactionRepository extends JpaRepository<Transactions, Long> {
    List<Transactions> findByUserId(String id);

}
