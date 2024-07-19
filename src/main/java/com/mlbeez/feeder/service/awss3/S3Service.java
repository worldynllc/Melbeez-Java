package com.mlbeez.feeder.service.awss3;
import java.util.ArrayList;
import java.util.List;
import com.mlbeez.feeder.service.IMediaStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;


import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

@Service("awss3")
public class S3Service implements IMediaStore {

	//It can be fetched form Database for each user
	@Value("${aws.s3.bucket}")
	String bucket;

	@Value("${aws.s3.secret}")
	String secretKey;

	@Value("${aws.s3.key}")
	String accessKeyId;
	@Value("${aws.region}")
	String region;

	@PostConstruct
	protected void init(){
		System.setProperty("aws.region", region);
	}

	private AwsCredentials getCredentials() {
		return new AwsCredentials() {
			@Override
			public String accessKeyId() {
				return accessKeyId;
			}
			@Override
			public String secretAccessKey() {
				return secretKey;
			}
		};
	}

	private AwsCredentialsProvider getCredentialsProvider() {
		return new AwsCredentialsProvider() {
			@Override
			public AwsCredentials resolveCredentials() {
				return getCredentials();
			}
		};
	}

	public String uploadFile(String filename, InputStream inputStream)
			throws  IOException {
		
		//Needs to put it as a singletop, one client instance is good for connection S3
		S3Client client = S3Client.builder().credentialsProvider(getCredentialsProvider()).build();
		PutObjectRequest request = PutObjectRequest.builder()
										.bucket(bucket)
										.key(filename)
										.acl("public-read")
										.build();
		client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));
		S3Waiter waiter = client.waiter();
		HeadObjectRequest waitRequest = HeadObjectRequest.builder()
											.bucket(bucket)
											.key(filename)
											.build();
		WaiterResponse<HeadObjectResponse> waitResponse = waiter.waitUntilObjectExists(waitRequest);
		AtomicReference<String> url = new AtomicReference<>("");
		waitResponse.matched().response().ifPresent(x -> {
			url.set(client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket ).key(filename).build()).toExternalForm());
		});
		return url.get();
	}
	
	
	
	
	//Get all the images from S3
	public List<String> getAllImageFileKeys() {
	    S3Client client = S3Client.builder().credentialsProvider(getCredentialsProvider()).build();
	    ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
	                .bucket(bucket)
	                .build();

	        List<String> imageKeys = new ArrayList<>();

	        ListObjectsV2Response listResponse;
	        do {
	            listResponse = client.listObjectsV2(listRequest);
	            listResponse.contents().forEach(object -> {
	                // Add the key to the list if it's an image file
	                if (object.key().toLowerCase().endsWith(".jpeg") ||
	                    object.key().toLowerCase().endsWith(".jpg") ||
	                    object.key().toLowerCase().endsWith(".png")) {
	                    imageKeys.add(object.key());
	                }
	            });
	            listRequest = ListObjectsV2Request.builder()
	                    .bucket(bucket)
	                    .continuationToken(listResponse.nextContinuationToken())
	                    .build();
	        } while (listResponse.isTruncated());

	        return imageKeys;
	    }

	   
	

	@Override
	public String getFileLocation(String filename) {
		S3Client client = S3Client.builder().credentialsProvider(getCredentialsProvider()).build();
		return client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket ).key(filename).build()).toExternalForm();
	}

	@Override
	public boolean deleteFile(final String filename) {
		S3Client client = S3Client.builder().credentialsProvider(getCredentialsProvider()).build();
		DeleteObjectResponse deleteObjectResponse = client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(filename).build());
		return true;
	}

}
