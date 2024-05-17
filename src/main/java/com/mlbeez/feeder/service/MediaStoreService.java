package com.mlbeez.feeder.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MediaStoreService {

    @Autowired
    ApplicationContext context;
    
   

    @Value("${media.store}")
    String mediaStore;
   
    public IMediaStore getMediaStoreService() {
        return (IMediaStore)context.getBean(Optional.ofNullable(MediaStoreEnum.getStore(mediaStore)).orElseThrow().getName());
    }




}
