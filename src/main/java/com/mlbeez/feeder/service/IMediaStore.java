package com.mlbeez.feeder.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IMediaStore {

    public String uploadFile(String fileName, InputStream inputStream,String folderName) throws IOException;

    public String getFileLocation(String filename);

    public boolean deleteFile(String filename);

    public List<String> getAllImageFileKeys();


}
