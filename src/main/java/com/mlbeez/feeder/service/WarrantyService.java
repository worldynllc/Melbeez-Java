package com.mlbeez.feeder.service;

import com.mlbeez.feeder.controller.WarrantyController;

import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;


import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.exception.ConstraintViolationException;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.mlbeez.feeder.service.exception.InternalServerException;


import com.stripe.exception.StripeException;
import com.stripe.model.Product;
import com.stripe.param.ProductCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


@Service
public class WarrantyService {

    @Autowired
   private WarrantyRepository warrantyRepository;


    @Autowired
    private MediaStoreService mediaStoreService;

    private static final Logger logger= LoggerFactory.getLogger(WarrantyService.class);

    public ResponseEntity<String> createWarranty(Warranty warranty, MultipartFile multipart) throws StripeException {
        String fileName = multipart.getOriginalFilename();
        String[] partStrings = fileName.split("\\.");
        String file = partStrings[0];
        String extension = (partStrings.length > 1) ? partStrings[1] : "";
        file = UUID.randomUUID().toString() + "." + extension;
        String message = "";
        Map<String, String> metadata = new HashMap<>();

        metadata.put("vendor", warranty.getVendor());
        metadata.put("warrantyId", warranty.getWarrantyId());
        metadata.put("planName", warranty.getPlanName());
        metadata.put("status", warranty.getStatus());
        metadata.put("pictureLink", warranty.getPictureLink());
        metadata.put("pictureName", warranty.getPictureName());
        metadata.put("discount", warranty.getDiscount());
        metadata.put("monthlyPrice",warranty.getMonthlyPrice());

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
            logger.error("Error uploading file: " + ex.getMessage(),ex);
            message = "Error uploading file: " + ex.getMessage();
        }

        //create the warranty product in Stripe
        ProductCreateParams productParams = ProductCreateParams.builder()
                .setName(warranty.getName())
                .setDescription(warranty.getPlanDescription())
                .putAllMetadata(metadata)
                .build();
        Product product = Product.create(productParams);

        warranty.setProductId(product.getId());
        warrantyRepository.save(warranty);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    public void deleteWarrantyById(Long id) {

        if(id==null){
            logger.error("Required warranty Id");
        }
        try{

            Optional<Warranty> optionalWarranty = warrantyRepository.findById(id);
            if (optionalWarranty.isPresent()) {
                Warranty warranty = optionalWarranty.get();
                if (!warranty.getPicture().isEmpty()) {
                    mediaStoreService.getMediaStoreService().deleteFile(warranty.getPicture());
                }
                warrantyRepository.deleteById(id);
            } else {
                logger.error("Warranty not found with id: " + id);
            }

        }
        catch (Exception e){
            logger.error("Internal error occurred while retrieving warranty",e);
            throw new InternalServerException("Internal error occurred while retrieving warranty with id: "+ id, e);
        }

    }

    public List<Warranty> getWarranty() {
        try{
            List<Warranty> warranty = warrantyRepository.findAll();
            if(warranty.isEmpty()){
                logger.error("Warranty not found");
                throw new DataNotFoundException("Warranty not found");
            }
            for (Warranty warranty1 : warranty) {
                Link selfLink = WebMvcLinkBuilder.linkTo(WarrantyController.class).withSelfRel();
                warranty1.add(selfLink);
            }
            Link link = WebMvcLinkBuilder.linkTo(WarrantyController.class).withSelfRel();
            CollectionModel<Warranty> result = CollectionModel.of(warranty, link);
            return warranty;

        }
        catch (Exception e){
            logger.error("Internal error occurred while retrieving warranty");
            throw new InternalServerException("Internal error occurred while retrieving warranty",e);
        }

    }

    public List<Warranty> getPendingWarranties() {
        return warrantyRepository.findByStatus("Pending");
    }

    public Optional<Warranty> updateWarranty(Long id, UpdateWarrantyRequest request) {

        try{
            return warrantyRepository.findById(id)
                    .map(existingWarranty -> {
                        if (request.getVendor() != null) existingWarranty.setVendor(request.getVendor());
                        if (request.getName() != null) existingWarranty.setName(request.getName());
                        if (request.getWarrantyId() != null) existingWarranty.setWarrantyId(request.getWarrantyId());
                        if (request.getMonthlyPrice() != null) existingWarranty.setMonthlyPrice(String.valueOf(request.getMonthlyPrice()));
                        if (request.getAnnualPrice() != null) existingWarranty.setAnnualPrice(request.getAnnualPrice());
                        if (request.getDiscount() != null) existingWarranty.setDiscount(String.valueOf(request.getDiscount()));
                        if (request.getPlanName()!=null) existingWarranty.setPlanName(request.getPlanName());
                        if (request.getPlanDescription()!=null) existingWarranty.setPlanDescription(request.getPlanDescription());
                        if (request.getUpdated_by() != null) existingWarranty.setUpdated_by(request.getUpdated_by());
                        if (request.getProduct_price_ids() != null) existingWarranty.setProduct_price_ids(request.getProduct_price_ids());
                        if (request.getOther_Details() != null) existingWarranty.setOther_Details(request.getOther_Details());
                        if (request.getStatus() != null) existingWarranty.setStatus((request.getStatus()));
                        return warrantyRepository.save(existingWarranty);
                    })
                    .map(Optional::of)
                    .orElseThrow(() -> new DataNotFoundException("Warranty not found with id: " + id));
        }
       catch (Exception ex){
            logger.error("Database constraint violation",ex);
            throw new ConstraintViolationException("Database constraint violation: " + ex.getMessage());
       }
    }
}