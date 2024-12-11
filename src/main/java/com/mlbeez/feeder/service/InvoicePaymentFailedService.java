package com.mlbeez.feeder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mlbeez.feeder.model.PaymentFailed;
import com.mlbeez.feeder.model.TransactionDto;
import com.mlbeez.feeder.model.Transactions;
import com.mlbeez.feeder.model.User;
import com.mlbeez.feeder.repository.CardDetailsRepository;
import com.mlbeez.feeder.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InvoicePaymentFailedService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentFailedService paymentFailedService;

    @Autowired
    private CardDetailsRepository cardDetailsRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WebhookService webhookService;

    private final Logger logger= LoggerFactory.getLogger(InvoicePaymentFailedService.class);

    public void handleInvoicePaymentFailed(Invoice invoice) throws StripeException {
        if (invoice == null) {
            logger.error("Invoice is null in invoice.payment_failed event");
            return;
        }
        User userDetail = userRepository.findByCustomerId(invoice.getCustomer());
        if (userDetail == null) {
            logger.error("User not found for customerId: {}", invoice.getCustomer());
            return;
        }
        Charge charge = Charge.retrieve(invoice.getCharge());
        try {
            PaymentFailed paymentFailed = new PaymentFailed();
            paymentFailed.setCustomer(invoice.getCustomer());
            paymentFailed.setEmail(charge.getBillingDetails().getEmail());
            paymentFailed.setName(charge.getBillingDetails().getName());
            paymentFailed.setStatus(charge.getStatus());
            paymentFailed.setReason(charge.getOutcome().getReason());
            paymentFailed.setFailure_code(charge.getFailureCode());
            paymentFailedService.toStore(paymentFailed);

        } catch (Exception e) {
            logger.error("Failed to store paymentFailed: {}", e.getMessage(), e);
        }

        logger.info("PaymentMethod ID from PaymentIntent: {}", charge.getPaymentMethod());

        if (charge.getPaymentMethod() != null) {
            PaymentMethod paymentMethod = retrievePaymentMethod(charge.getPaymentMethod());
            if (paymentMethod != null) {
                webhookService. saveCardDetails(paymentMethod, userDetail);
                String productId = invoice.getLines().getData().get(0).getPlan().getProduct();

                Product product = Product.retrieve(productId);

                try{
                    if (paymentMethod.getId() != null) {
                        Transactions transaction = new Transactions();
                        transaction.setUserId(userDetail.getUserId());
                        transaction.setProductName(product.getName());
                        transaction.setCard(paymentMethod.getCard().getLast4());
                        transaction.setPrice(invoice.getLines().getData().get(0).getPlan().getAmount());
                        transaction.setPaymentMethod(paymentMethod.getType());
                        transaction.setChargeRequest_status(charge.getStatus());
                        transaction.setInvoice_status(invoice.getStatus());
                        transaction.setReceiptUrl("");
                        transaction.setCustomerId(invoice.getCustomer());
                        transaction.setEmail(userDetail.getEmail());
                        transaction.setPhoneNumber(userDetail.getPhoneNumber());
                        transaction.setProductId(productId);
                        transaction.setTransactionId(charge.getId());
                        transaction.setInterval(invoice.getLines().getData().get(0).getPrice().getRecurring().getInterval());

                        transactionService.storeHistory(transaction);

                        TransactionDto dto=transaction.toLogDTO();
                        ObjectMapper objectMapper=new ObjectMapper();
                        objectMapper.registerModule(new JavaTimeModule());
                        try{
                            String paymentFailedJson=objectMapper.writeValueAsString(dto);
                            logger.error("PaymentFailed entity details: {}",paymentFailedJson);
                        }catch (JsonProcessingException e)
                        {
                            logger.error("Error converting PaymentFailed object to JSON", e);
                        }
                    }
                }
                catch (Exception e){
                    logger.error("Failed to store transaction details for charge ID: {}. Error: {}", charge.getId(), e.getMessage(), e);
                }

            }
        }
    }

    private PaymentMethod retrievePaymentMethod(String paymentMethodId) {
        try {
            return PaymentMethod.retrieve(paymentMethodId);
        } catch (StripeException e) {
            logger.error("Error retrieving payment method: {}", e.getMessage());
            return null;
        }
    }

}
