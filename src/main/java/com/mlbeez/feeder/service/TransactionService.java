package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.Transactions;

import com.mlbeez.feeder.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public void storeHistory(Transactions transcations)
    {
         transactionRepository.save(transcations);
    }

    public List<Transactions> getData(String userId)
    {
        return transactionRepository.findByUserId(userId);
    }
}
