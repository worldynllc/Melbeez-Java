package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.exception.InvalidWarrantyException;
import com.mlbeez.feeder.service.exception.WarrantyNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WarrantyService {


    @Autowired
    private WarrantyRepository warrantyRepository;

    public Warranty createWarranty(Warranty warranty) {
        if (warranty == null || warranty.getName() == null) {
            throw new InvalidWarrantyException("Invalid warranty details provided");
        }
        return warrantyRepository.save(warranty);
    }

    public void deleteWarrantyById(Long id) {
        if (!warrantyRepository.existsById(id)) {
            throw new WarrantyNotFoundException("Warranty not found with id: " + id);
        }
        warrantyRepository.deleteById(id);
    }

    public Optional<Warranty> getWarrantyById(Long id) {
        Optional<Warranty> warranty = warrantyRepository.findById(id);
        if (!warranty.isPresent()) {
            throw new WarrantyNotFoundException("Warranty not found with id: " + id);
        }
        return warranty;
    }

    public List<Warranty> getWarranty() {
        return warrantyRepository.findAll();
    }

    public List<Warranty> getPendingWarranties() {
        return warrantyRepository.findByStatus("Pending");
    }


    public Optional<Warranty> updateWarranty(Long id, UpdateWarrantyRequest request) {
        return warrantyRepository.findById(id)
                .map(existingWarranty -> {
                    if (request.getVendor() != null) existingWarranty.setVendor(request.getVendor());
                    if (request.getName() != null) existingWarranty.setName(request.getName());
                    if (request.getWarrantyId() != null) existingWarranty.setWarrantyId(request.getWarrantyId());
                    if (request.getMonthlyPrice() != null) existingWarranty.setMonthlyPrice(request.getMonthlyPrice());
                    if (request.getAnnualPrice() != null) existingWarranty.setAnnualPrice(request.getAnnualPrice());
                    if (request.getDiscount() != null) existingWarranty.setDiscount(request.getDiscount());
                    if (request.getPlanName() != null) existingWarranty.setPlanName(request.getPlanName());
                    if (request.getPlanDescription() != null)
                        existingWarranty.setPlanDescription(request.getPlanDescription());
//                if (request.getCreatedBy() != null) existingWarranty.setCreated_by(request.getCreatedBy());
                    if (request.getUpdated_by() != null) existingWarranty.setUpdated_by(request.getUpdated_by());
                    if (request.getStatus() != null) existingWarranty.setStatus((request.getStatus()));

                    return warrantyRepository.save(existingWarranty);
                })
                .map(Optional::of)
                .orElseThrow(() -> new WarrantyNotFoundException("Warranty not found with id: " + id));
    }


}
