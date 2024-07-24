package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.WarrantyRepository;
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


@RestController
public class WarrantyController {
    @Autowired
    private WarrantyService warrantyService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    private static final Logger logger = LoggerFactory.getLogger(WarrantyController.class);

    @PostMapping("warranty/upload")
    public ResponseEntity<String> warrantyUpload(@ModelAttribute Warranty warranty, @RequestParam("file") MultipartFile multipart) {
        logger.debug("Request to Upload Warranty {}", warranty);
        return warrantyService.createWarranty(warranty, multipart);
    }

    @DeleteMapping("warranty/{id}")
    public ResponseEntity<String> deleteWarrantyById(@PathVariable("id") Long id) {
        logger.debug("Request to Delete Warranty {}", id);
        warrantyService.deleteWarrantyById(id);
        return ResponseEntity.status(HttpStatus.OK).body("Warranty Deleted");

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
        logger.debug("Request to Update Warranty {}",request);
        Optional<Warranty> result = warrantyService.updateWarranty(id, request);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
