package com.ziboto.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private Security security = new Security();
    private Storage storage = new Storage();
    private Cache cache = new Cache();
    
    @Data
    public static class Security {
        private Jwt jwt = new Jwt();
        private Cors cors = new Cors();
        
        @Data
        public static class Jwt {
            private String secret;
            private Long expiration;
            private Long refreshExpiration;
        }
        
        @Data
        public static class Cors {
            private String allowedOrigins;
            private String allowedMethods;
            private String allowedHeaders;
            private Boolean allowCredentials;
            private Long maxAge;
        }
    }
    
    @Data
    public static class Storage {
        private String type;
        private Local local = new Local();
        private S3 s3 = new S3();
        
        @Data
        public static class Local {
            private String basePath;
        }
        
        @Data
        public static class S3 {
            private String bucket;
            private String region;
            private String accessKey;
            private String secretKey;
        }
    }
    
    @Data
    public static class Cache {
        private Long ttl;
        private String prefix;
    }
}
