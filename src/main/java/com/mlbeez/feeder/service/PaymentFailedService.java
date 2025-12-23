package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.PaymentFailed;
import com.mlbeez.feeder.repository.PaymentFailedRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentFailedService {

    private final PaymentFailedRepository paymentFailedRepository;

    public PaymentFailedService(PaymentFailedRepository paymentFailedRepository) {
        this.paymentFailedRepository = paymentFailedRepository;
    }

    public void toStore(PaymentFailed paymentFailed)
    {
        paymentFailedRepository.save(paymentFailed);
    }
}
