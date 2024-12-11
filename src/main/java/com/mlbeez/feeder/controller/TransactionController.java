package com.mlbeez.feeder.controller;
import com.mlbeez.feeder.model.Transactions;
import com.mlbeez.feeder.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TransactionController {
    @Autowired
    TransactionService transactionService;
    @GetMapping("/history/{id}")
    public List<Transactions> getData(@PathVariable("id") String id)
    {
        return transactionService.getData(id);
    }
}
