package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;

import com.mlbeez.feeder.repository.WarrantyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WarrantyService {

    @Autowired
    private WarrantyRepository warrantyRepository;

    public Warranty createWarranty(Warranty warranty) {
        return warrantyRepository.save(warranty);
    }

    public void deleteWarrantyById(Long id) {
        warrantyRepository.deleteById(id);
    }

    public Optional<Warranty> getWarrantyById(Long id) {
        return warrantyRepository.findById(id);
    }

    public List<Warranty> getWarranty() {
        return warrantyRepository.findAll();
    }



    public Optional<Warranty> updateWarranty(Long id, UpdateWarrantyRequest request) {
        return warrantyRepository.findById(id)
                .map(existingWarranty -> {
                    if (request.getVendor() != null) existingWarranty.setVendor(request.getVendor());
                    if (request.getProductName() != null) existingWarranty.setProductName(request.getProductName());
                    if (request.getWarrantyId() != null) existingWarranty.setWarrantyId(request.getWarrantyId());
                    if (request.getMonthlyPrice() != null) existingWarranty.setMonthlyPrice(request.getMonthlyPrice());
                    if (request.getAnnualPrice() != null) existingWarranty.setAnnualPrice(request.getAnnualPrice());
                    if (request.getDiscount() != null) existingWarranty.setDiscount(request.getDiscount());
                    if (request.getTerms_conditions() != null) existingWarranty.setTerms_conditions(request.getTerms_conditions());
//                    if (request.getCreatedBy() != null) existingWarranty.setCreated_by(request.getCreatedBy());
                    if (request.getUpdated_by() != null) existingWarranty.setUpdated_by(request.getUpdated_by());
                    if (request.getStatus() != null) existingWarranty.setStatus(request.getStatus());

                    return warrantyRepository.save(existingWarranty);
                });
    }

}
