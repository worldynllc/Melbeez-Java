package com.mlbeez.feeder;

import com.mlbeez.feeder.model.Transactions;
import com.mlbeez.feeder.repository.TransactionRepository;
import com.mlbeez.feeder.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;


    @Test
    void testStoreHistory_Success() {
        Transactions transaction = new Transactions();
        transaction.setUserId("user123");
        transaction.setProductName("Product");
        transaction.setPrice(100L);
        transactionService.storeHistory(transaction);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void testGetData_Success() {
        String userId = "user123";
        Transactions transactionOne = new Transactions();
        transactionOne.setUserId(userId);
        Transactions transactionTwo = new Transactions();
        transactionTwo.setUserId(userId);
        when(transactionRepository.findByUserId(userId)).thenReturn(Arrays.asList(transactionOne, transactionTwo));
        List<Transactions> transactions = transactionService.getData(userId);
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        verify(transactionRepository).findByUserId(userId);
    }


    @Test
    void testGetAll_Success() {
        Transactions transactionOne = new Transactions();
        Transactions transactionTwo = new Transactions();
        when(transactionRepository.findAll()).thenReturn(Arrays.asList(transactionOne, transactionTwo));
        List<Transactions> transactions = transactionService.getAll();
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        verify(transactionRepository).findAll();
    }
}