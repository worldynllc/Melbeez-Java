package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.UpdateWarrentyRequest;
import com.mlbeez.feeder.model.Warrenty;
import com.mlbeez.feeder.repository.WarrentyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WarrentyService {

    @Autowired
    private WarrentyRepository warrentyRepository;

    public Warrenty createWarrenty(Warrenty warrenty) {
        return warrentyRepository.save(warrenty);
    }

    public void deleteWarrentyById(Long id) {
        warrentyRepository.deleteById(id);
    }

    public Optional<Warrenty> getWarrentyById(Long id) {
        return warrentyRepository.findById(id);
    }

    public List<Warrenty> getWarrenty() {
        return warrentyRepository.findAll();
    }



    public Optional<Warrenty> updateWarrenty(Long id, UpdateWarrentyRequest request) {
        return warrentyRepository.findById(id)
                .map(existingWarrenty -> {
                    if (request.getVendor() != null) existingWarrenty.setVendor(request.getVendor());
                    if (request.getProductName() != null) existingWarrenty.setProductName(request.getProductName());
                    if (request.getMonthlyPrice() != null) existingWarrenty.setMonthlyPrice(request.getMonthlyPrice());
                    if (request.getAnnualPrice() != null) existingWarrenty.setAnnualPrice(request.getAnnualPrice());
                    if (request.getDiscount() != null) existingWarrenty.setDiscount(request.getDiscount());
                    if (request.getTermsConditions() != null) existingWarrenty.setTerms_conditions(request.getTermsConditions());
                    if (request.getCreatedBy() != null) existingWarrenty.setCreated_by(request.getCreatedBy());
                    if (request.getUpdatedBy() != null) existingWarrenty.setUpdated_by(request.getUpdatedBy());

                    return warrentyRepository.save(existingWarrenty);
                });
    }

}
