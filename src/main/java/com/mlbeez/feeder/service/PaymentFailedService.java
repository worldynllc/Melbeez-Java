package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.PaymentFailed;
import com.mlbeez.feeder.repository.PaymentFailedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentFailedService {

    @Autowired
    PaymentFailedRepository paymentFailedRepository;

    public PaymentFailed toStore(PaymentFailed paymentFailed)
    {
        return paymentFailedRepository.save(paymentFailed);
    }
}
