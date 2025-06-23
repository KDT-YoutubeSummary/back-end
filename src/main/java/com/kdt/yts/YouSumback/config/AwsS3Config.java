package com.kdt.yts.YouSumback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    // application.properties에 설정된 AWS 리전 값을 가져옵니다.
    @Value("${aws.region}")
    private String awsRegion;

    @Bean
    public S3Client s3Client() {
        // S3Client 객체를 생성해서 스프링 컨테이너에 Bean으로 등록
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}