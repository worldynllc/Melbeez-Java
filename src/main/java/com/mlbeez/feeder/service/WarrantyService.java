package com.mlbeez.feeder.service;

import com.mlbeez.feeder.controller.FeedController;
import com.mlbeez.feeder.controller.WarrantyController;
import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.exception.WarrantyNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WarrantyService {

    @Autowired
    private WarrantyRepository warrantyRepository;

    @Autowired
    private MediaStoreService mediaStoreService;

    public ResponseEntity<String> createWarranty(Warranty warranty, MultipartFile multipart) {
        String fileName = multipart.getOriginalFilename();
        String[] partStrings = fileName.split("\\.");
        String file = partStrings[0];
        String extension = (partStrings.length > 1) ? partStrings[1] : "";
        file = UUID.randomUUID().toString() + "." + extension;
        String message = "";

        try {
            String s = mediaStoreService.getMediaStoreService().uploadFile(file, multipart.getInputStream());
            if (multipart.isEmpty()) {
                warranty.setPictureName("");
                warranty.setPicture("");
                warranty.setPictureLink("");
                warranty.getCreatedAt();
                warrantyRepository.save(warranty);
                message = "Your file has been uploaded successfully! here ";
            } else {
                warranty.setPictureName(fileName);
                warranty.setPicture(file);
                warranty.setPictureLink(s);
                warrantyRepository.save(warranty);
                message = "Your file has been uploaded successfully! here " + s;
            }
        } catch (Exception ex) {
            message = "Error uploading file: " + ex.getMessage();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    public void deleteWarrantyById(Long id) {
        Optional<Warranty> optionalWarranty = warrantyRepository.findById(id);
        if (optionalWarranty.isPresent()) {
            Warranty warranty = optionalWarranty.get();
            if (!warranty.getPicture().isEmpty()) {
                mediaStoreService.getMediaStoreService().deleteFile(warranty.getPicture());
            }
            warrantyRepository.deleteById(id);
        } else {
            throw new WarrantyNotFoundException("Warranty not found with id: " + id);
        }
    }

    public Optional<Warranty> getWarrantyById(Long id) {
        Optional<Warranty> warranty = warrantyRepository.findById(id);
        if (warranty.isEmpty()) {
            throw new WarrantyNotFoundException("Warranty not found with id: " + id);
        }
        return warranty;
    }

    public List<Warranty> getWarranty() {
        List<Warranty> warranty = warrantyRepository.findAll();
        for (Warranty warranty1 : warranty) {
            Link selfLink = WebMvcLinkBuilder.linkTo(WarrantyController.class).withSelfRel();
            warranty1.add(selfLink);
        }
        Link link = WebMvcLinkBuilder.linkTo(FeedController.class).withSelfRel();
        CollectionModel<Warranty> result = CollectionModel.of(warranty, link);
        return warranty;
    }

    public List<Warranty> getPendingWarranties() {
        return warrantyRepository.findByStatus("Pending");
    }

    public Optional<Warranty> updateWarranty(Long id, UpdateWarrantyRequest request, MultipartFile multipart) {
        return warrantyRepository.findById(id)
                .map(existingWarranty -> {
                    if (request.getVendor() != null) existingWarranty.setVendor(request.getVendor());
                    if (request.getName() != null) existingWarranty.setName(request.getName());
                    if (request.getWarrantyId() != null) existingWarranty.setWarrantyId(request.getWarrantyId());
                    if (request.getMonthlyPrice() != null) existingWarranty.setMonthlyPrice(request.getMonthlyPrice());
                    if (request.getAnnualPrice() != null) existingWarranty.setAnnualPrice(request.getAnnualPrice());
                    if (request.getDiscount() != null) existingWarranty.setDiscount(request.getDiscount());
                    if (request.getPlanName() != null) existingWarranty.setPlanName(request.getPlanName());
                    if (request.getPlanDescription() != null) existingWarranty.setPlanDescription(request.getPlanDescription());
                    if (request.getUpdated_by() != null) existingWarranty.setUpdated_by(request.getUpdated_by());
                    if (request.getStatus() != null) existingWarranty.setStatus((request.getStatus()));
                    return warrantyRepository.save(existingWarranty);
                })
                .map(Optional::of)
                .orElseThrow(() -> new WarrantyNotFoundException("Warranty not found with id: " + id));
    }
}



