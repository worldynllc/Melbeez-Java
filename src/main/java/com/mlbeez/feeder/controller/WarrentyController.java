package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.UpdateWarrentyRequest;
import com.mlbeez.feeder.model.Warrenty;
import com.mlbeez.feeder.repository.WarrentyRepository;
import com.mlbeez.feeder.service.MediaStoreService;
import com.mlbeez.feeder.service.WarrentyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController

public class WarrentyController {

    @Autowired
    private MediaStoreService mediaStoreService;

    @Autowired
    private WarrentyService warrentyService;

    @Autowired
    private WarrentyRepository warrentyRepository;

    @PostMapping("warrenty/upload")
    public Warrenty warrentyUpload(@RequestBody Warrenty warrenty) {
        return warrentyService.createWarrenty(warrenty);
    }

    @DeleteMapping("warrenty/{id}")
    public ResponseEntity<?> deleteWarrentyById(@PathVariable("id") Long id) {
        Optional<Warrenty> optionalWarrenty = warrentyService.getWarrentyById(id);

        if (optionalWarrenty.isPresent()) {
            warrentyService.deleteWarrentyById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("warrenty/all")
    public List<Warrenty> getWarrentyAll() {
        return warrentyService.getWarrenty();
    }

    @PutMapping("warrenty/{id}")
    public ResponseEntity<Warrenty> updateWarrenty(
            @PathVariable Long id,
            @RequestBody UpdateWarrentyRequest request
    ) {
        Optional<Warrenty> result = warrentyService.updateWarrenty(id, request);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
