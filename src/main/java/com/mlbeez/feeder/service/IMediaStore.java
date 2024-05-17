package com.mlbeez.feeder.service;

import java.io.IOException;
import java.io.InputStream;

public interface IMediaStore {

    public String uploadFile(String fileName, InputStream inputStream) throws IOException;

    public String getFileLocation(String filename);

    public boolean deleteFile(String filename);
}
