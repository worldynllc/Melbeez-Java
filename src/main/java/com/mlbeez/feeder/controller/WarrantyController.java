package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.MediaStoreService;
import com.mlbeez.feeder.service.WarrantyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController

public class WarrantyController {

    @Autowired
    private MediaStoreService mediaStoreService;

    @Autowired
    private WarrantyService warrantyService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    private static final Logger logger = LoggerFactory.getLogger(WarrantyController.class);

    @PostMapping("warranty/upload")
    public ResponseEntity<String> warrantyUpload(@ModelAttribute Warranty warranty, @RequestParam("file") MultipartFile multipart) {
        logger.debug("Request to Upload Warranty {}", warranty);

        String orginalFilename = multipart.getOriginalFilename();
        String fileName = multipart.getOriginalFilename();
        String[] partStrings = fileName.split("\\.");
        String file = partStrings[0];
        String extension = (partStrings.length > 1) ? partStrings[1] : "";
        fileName = UUID.randomUUID().toString() + "." + extension;
        String message = "";

        try {
            String s = mediaStoreService.getMediaStoreService().uploadFile(fileName, multipart.getInputStream());
            if (multipart.isEmpty()) {
                warranty.setPictureName("");
                warranty.setPicture("");
                warranty.setPictureLink("");
                warranty.getCreatedAt();
                warrantyService.createWarranty(warranty);
                message = "Your file has been uploaded successfully! here ";
            } else {
                warranty.setPictureName(orginalFilename);
                warranty.setPicture(fileName);
                warranty.setPictureLink(s);
                warrantyService.createWarranty(warranty);
                message = "Your file has been uploaded successfully! here " + s;
            }
        } catch (Exception ex) {
            message = "Error uploading file: " + ex.getMessage();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @DeleteMapping("warranty/{id}")
    public ResponseEntity<String> deleteWarrantyById(@PathVariable("id") Long id) {
        logger.debug("Request to Delete Warranty {}", id);
        Optional<Warranty> optionalWarranty = warrantyService.getWarrantyById(id);

        if (optionalWarranty.isPresent()) {
            Warranty warranty = optionalWarranty.get();
            if (!warranty.getPicture().isEmpty()) {
                mediaStoreService.getMediaStoreService().deleteFile(warranty.getPicture());
            }
            warrantyService.deleteWarrantyById(id);
            return ResponseEntity.status(HttpStatus.CREATED).body("Warranty Deleted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Warranty Not Found");
        }
    }

    @GetMapping("/warranty/pending")
    public List<Warranty> getPending() {
        logger.debug("Request to GetAll Pending Warranty");
        return warrantyService.getPendingWarranties();
    }

    @GetMapping("warranty/all")
    public List<Warranty> getWarrantyAll() {
        logger.debug("Request to GetAll Warranty");
        return warrantyService.getWarranty();
    }

    @GetMapping("/warranty/{id}")
    public ResponseEntity<Optional<Warranty>> getWarrantyId(@PathVariable("id") Long id) {
        logger.debug("Request to Get Warranty by Id");
        Optional<Warranty> warranty = warrantyService.getWarrantyById(id);
        return ResponseEntity.ok().body(warranty);
    }

    @PutMapping("warranty/{id}")
    public ResponseEntity<Warranty> updateWarranty(@PathVariable Long id, @RequestBody UpdateWarrantyRequest request) {
        logger.debug("Request to Update Warranty {}", request);
        Optional<Warranty> result = warrantyService.updateWarranty(id, request);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
