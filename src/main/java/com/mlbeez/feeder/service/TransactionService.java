package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.Transactions;

import com.mlbeez.feeder.repository.TransactionRepository;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public void storeHistory(Transactions transcations) {
        transactionRepository.save(transcations);
    }

    public List<Transactions> getData(String userId) {
        if (!userId.isEmpty()) {
            return transactionRepository.findByUserId(userId);
        } else {
            logger.error("userId not found :" + userId);
            throw new DataNotFoundException("userId not found :" + userId);
        }

    }

    public List<Transactions> getAll() {
        return transactionRepository.findAll();
    }
}
