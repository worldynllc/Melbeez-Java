package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;

import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.MediaStoreService;
import com.mlbeez.feeder.service.WarrantyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController

public class WarrantyController {

    @Autowired
    private MediaStoreService mediaStoreService;

    @Autowired
    private WarrantyService warrantyService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    @PostMapping("warranty/upload")
    public Warranty warrantyUpload(@RequestBody Warranty warranty) {
        return warrantyService.createWarranty(warranty);
    }

    @DeleteMapping("warranty/{id}")
    public ResponseEntity<?> deleteWarrantyById(@PathVariable("id") Long id) {
        Optional<Warranty> optionalWarranty = warrantyService.getWarrantyById(id);

        if (optionalWarranty.isPresent()) {
            warrantyService.deleteWarrantyById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("warranty/all")
    public List<Warranty> getWarrantyAll() {
        return warrantyService.getWarranty();
    }

    @PutMapping("warranty/{id}")
    public ResponseEntity<Warranty> updateWarranty(
            @PathVariable Long id,
            @RequestBody UpdateWarrantyRequest request
    ) {
        Optional<Warranty> result = warrantyService.updateWarranty(id, request);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
