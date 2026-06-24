package com.smartcampus.library;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.xml.ws.Endpoint;

@Configuration
public class CxfConfig {

    @Autowired
    private Bus bus;

    @Autowired
    private LibrarySoapService librarySoapService;

    @Bean
    public Endpoint endpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, librarySoapService);

        // This publishes our service WSDL at
        // http://localhost:8083/services/booking?wsdl
        endpoint.publish("/booking");

        return endpoint;
    }
}