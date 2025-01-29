package com.mlbeez.feeder;

import com.mlbeez.feeder.model.UpdateWarrantyRequest;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.IMediaStore;
import com.mlbeez.feeder.service.MediaStoreService;
import com.mlbeez.feeder.service.WarrantyService;
import com.stripe.Stripe;
import com.stripe.model.Product;
import com.stripe.param.ProductCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WarrantyTest {


    @InjectMocks
    private WarrantyService warrantyService;

    @Mock
    private WarrantyRepository warrantyRepository;

    @Mock
    private MediaStoreService mediaStoreService;

    @Mock
    private IMediaStore mediaStore;

    @Value("${stripe.api.key}")
    private String apiKey;

    @BeforeEach
    void setUp() {
        Stripe.apiKey = apiKey;
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateWarranty_Success() throws Exception {
        Warranty warranty = new Warranty();
        warranty.setVendor("Vendor1");
        warranty.setWarrantyId("W123");
        warranty.setPlanName("Plan A");
        warranty.setStatus("Active");
        warranty.setPictureLink("http://example.com/pic.jpg");
        warranty.setPictureName("pic.jpg");
        warranty.setDiscount("10%");
        warranty.setMonthlyPrice("50");
        warranty.setName("Sample Warranty");
        warranty.setPlanDescription("Sample Plan Description");
        warranty.setUpdated_by("Admin");

        String fileName = "testfile.jpg";
        MockMultipartFile multipartFile = new MockMultipartFile("file", fileName, "image/jpeg", "Sample Content".getBytes());
        String folderName = "Admin-Warranty";
        String file = UUID.randomUUID().toString() + ".jpg";
        String uploadedFileLink = "https://s3.example.com/" + folderName + "/" + file;

        when(mediaStoreService.getMediaStoreService()).thenReturn(mediaStore);
        when(mediaStore.uploadFile(anyString(), any(InputStream.class), eq(folderName))).thenReturn(uploadedFileLink);

        try (MockedStatic<Product> mockedStaticProduct = Mockito.mockStatic(Product.class)) {
            mockedStaticProduct
                    .when(() -> Product.create(any(ProductCreateParams.class)))
                    .thenAnswer(invocation -> {
                        Product mockProduct = mock(Product.class);
                        when(mockProduct.getId()).thenReturn("prod_123");
                        return mockProduct;
                    });
            String result = warrantyService.createWarranty(warranty, multipartFile);
            assertEquals("Your file has been uploaded successfully! here " + uploadedFileLink, result);
            verify(warrantyRepository).save(warranty);
            verify(mediaStore).uploadFile(anyString(), any(InputStream.class), eq(folderName));
            ArgumentCaptor<ProductCreateParams> paramsCaptor = ArgumentCaptor.forClass(ProductCreateParams.class);
            mockedStaticProduct.verify(() -> Product.create(paramsCaptor.capture()));
            ProductCreateParams capturedParams = paramsCaptor.getValue();

            assertEquals(warranty.getName(), capturedParams.getName());
            assertEquals(warranty.getPlanDescription(), capturedParams.getDescription());
            assertEquals(warranty.getVendor(), capturedParams.getMetadata().get("vendor"));
            assertEquals(warranty.getWarrantyId(), capturedParams.getMetadata().get("warrantyId"));
        }
    }

    @Test
    void testGetWarrantyAll_Success() {
        Warranty warranty1 = new Warranty();
        warranty1.setId(1L);
        warranty1.setVendor("Vendor1");
        warranty1.setWarrantyId("W123");
        warranty1.setPlanName("Plan A");
        warranty1.setStatus("Active");
        warranty1.setPictureLink("http://example.com/pic.jpg");
        warranty1.setPictureName("pic.jpg");
        warranty1.setDiscount("10%");
        warranty1.setMonthlyPrice("50");
        warranty1.setName("Sample Warranty");
        warranty1.setPlanDescription("Sample Plan Description");
        warranty1.setUpdated_by("Admin");

        List<Warranty> warranties = List.of(warranty1);
        when(warrantyRepository.findAll()).thenReturn(warranties);

        List<Warranty> warrantyList = warrantyService.getWarranty();

        assertEquals(1, warrantyList.size());
        Warranty returnedWarranty = warrantyList.get(0);
        assertEquals(warranty1.getId(), returnedWarranty.getId());
        assertEquals(warranty1.getVendor(), returnedWarranty.getVendor());
        assertEquals(warranty1.getWarrantyId(), returnedWarranty.getWarrantyId());
        assertEquals(warranty1.getPlanName(), returnedWarranty.getPlanName());
        assertEquals(warranty1.getStatus(), returnedWarranty.getStatus());
        assertEquals(warranty1.getPictureLink(), returnedWarranty.getPictureLink());
        assertEquals(warranty1.getPictureName(), returnedWarranty.getPictureName());
        assertEquals(warranty1.getDiscount(), returnedWarranty.getDiscount());
        assertEquals(warranty1.getMonthlyPrice(), returnedWarranty.getMonthlyPrice());
        assertEquals(warranty1.getName(), returnedWarranty.getName());
        assertEquals(warranty1.getPlanDescription(), returnedWarranty.getPlanDescription());
        assertEquals(warranty1.getUpdated_by(), returnedWarranty.getUpdated_by());
        verify(warrantyRepository).findAll();
    }

    @Test
    void testDeleteWarrantyById_Success() {
        Warranty warranty = new Warranty();
        warranty.setId(1L);
        warranty.setVendor("Vendor1");
        warranty.setWarrantyId("W123");
        warranty.setPlanName("Plan A");
        warranty.setStatus("Active");
        warranty.setPicture("http://example.com/pic.jpg");
        warranty.setPictureLink("http://example.com/linkss.jpg");
        warranty.setPictureName("pic.jpg");
        warranty.setDiscount("10%");
        warranty.setMonthlyPrice("50");
        warranty.setName("Sample Warranty");
        warranty.setPlanDescription("Sample Plan Description");
        warranty.setUpdated_by("Admin");
        when(mediaStoreService.getMediaStoreService()).thenReturn(mediaStore);
        when(mediaStore.deleteFile(warranty.getPicture())).thenReturn(true);

        warrantyService.deleteWarranty(warranty, warranty.getId());

        verify(mediaStore, times(1)).deleteFile(warranty.getPicture());
        verify(warrantyRepository, times(1)).deleteById(warranty.getId());
    }

    @Test
    void testUpdateWarranty() {
        Long id = 1L;
        Warranty existingWarranty = new Warranty();
        existingWarranty.setId(id);
        existingWarranty.setName("Kavi");

        UpdateWarrantyRequest request = new UpdateWarrantyRequest();
        request.setName("Ravi");
        request.setVendor("Raj-vendor");

        when(warrantyRepository.findById(id)).thenReturn(Optional.of(existingWarranty));
        when(warrantyRepository.save(any(Warranty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Warranty> updatedWarranty = warrantyService.updateWarranty(id, request);

        assertTrue(updatedWarranty.isPresent());
        Warranty result = updatedWarranty.get();
        assertEquals("Ravi", result.getName());
        assertEquals("Raj-vendor", result.getVendor());

        verify(warrantyRepository).findById(id);
        verify(warrantyRepository).save(existingWarranty);

        ArgumentCaptor<Warranty> captor = ArgumentCaptor.forClass(Warranty.class);
        verify(warrantyRepository).save(captor.capture());
        Warranty capturedWarranty = captor.getValue();
        assertEquals("Ravi", capturedWarranty.getName());
        assertEquals("Raj-vendor", capturedWarranty.getVendor());
    }


}
