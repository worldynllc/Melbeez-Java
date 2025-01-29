package com.mlbeez.feeder.controller;
import com.mlbeez.feeder.model.Transactions;
import com.mlbeez.feeder.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    private static final Logger logger= LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    TransactionService transactionService;

    @GetMapping("/user/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
    public List<Transactions> getData(@PathVariable("id") String id)
    {
        logger.info("Request to get transactions from user");
        return transactionService.getData(id);
    }

    @GetMapping("/user/all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public List<Transactions> getAll(){
        logger.info("Request to get all transactions from transaction table");
        return transactionService.getAll();
    }
}
