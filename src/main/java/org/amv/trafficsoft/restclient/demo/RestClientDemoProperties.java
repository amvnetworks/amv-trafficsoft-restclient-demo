package org.amv.trafficsoft.restclient.demo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "amv.trafficsoft.restclient.demo")
public class RestClientDemoProperties {
    private List<Long> vehicleIds;
}
