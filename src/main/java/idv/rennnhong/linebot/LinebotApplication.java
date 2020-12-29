package idv.rennnhong.linebot;

import com.google.maps.GeoApiContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class LinebotApplication {
    public static Path downloadedContentDir;

    public static void main(String[] args) throws IOException {
        downloadedContentDir = Files.createTempDirectory("line-bot");
        SpringApplication.run(LinebotApplication.class, args);

    }

    @Bean
    public GeoApiContext context() {
        return new GeoApiContext.Builder()
                .apiKey("input your own key")
                .build();
    }

//    @Bean
//    public CommonsRequestLoggingFilter requestLoggingFilter() {
//        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
//        loggingFilter.setIncludeClientInfo(true);
//        loggingFilter.setIncludeQueryString(true);
//        loggingFilter.setIncludePayload(true);
//        loggingFilter.setMaxPayloadLength(64000);
//        return loggingFilter;
//    }

}
