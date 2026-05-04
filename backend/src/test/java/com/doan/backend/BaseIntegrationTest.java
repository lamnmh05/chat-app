package com.doan.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // Khởi tạo 1 container dùng chung duy nhất
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    static {
        // VŨ KHÍ TỐI THƯỢNG CHO MAC APPLE SILICON:
        // Ép Java phân giải mạng bằng IPv4 (127.0.0.1) thay vì IPv6 (::1).
        // Phải đặt dòng này trước khi container và Spring Boot khởi động!
        System.setProperty("java.net.preferIPv4Stack", "true");

        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // Trả lại đường dẫn gốc, để Testcontainers tự cấu hình Replica Set
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}