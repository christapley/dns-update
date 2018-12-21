/*
 * Copyright 2018 Chris.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hexagon.geospatial.infrastructure.dns.update;

import com.hexagon.geospatial.infrastructure.dns.update.client.DnsClient;
import com.hexagon.geospatial.infrastructure.dns.update.entity.DnsEntry;
import com.hexagon.geospatial.infrastructure.dns.update.storage.DnsEntryStorage;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Chris
 */
@RestController
@CrossOrigin
public class DnsUpdateController {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(DnsUpdateController.class);
    
    @Autowired
    DnsEntryStorage dnsEntriesStorage;
    
    @Autowired
    DnsClient dnsClient;
    
    @GetMapping("/register/{fqdn}/{ipAddress}")
    @ResponseBody
    public ResponseEntity<DnsEntry> register(@PathVariable("fqdn") String fqdn,
            @PathVariable("ipAddress") String ipAddress) throws IOException {
        
        LOGGER.info("Received request to register %s as %s", fqdn, ipAddress);
        
        DnsEntry dnsEntry = new DnsEntry(ipAddress, fqdn);
        
        dnsEntriesStorage.addDnsEntry(dnsEntry);
        
        return ResponseEntity.ok().body(dnsEntry);
    }
    
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<List<DnsEntry>> listAll() throws IOException {
        return ResponseEntity.ok().body(dnsEntriesStorage.listAllDnsEnrties());
    }
    
    @Scheduled(fixedRate = 30000)
    public void updateDns() throws IOException, Exception {
        List<DnsEntry> currentDnsEntries = dnsEntriesStorage.listAllDnsEnrties();
        currentDnsEntries.stream().forEach((dnsEntry) -> {
            try {
                dnsClient.UpdateARecordEntry(dnsEntry);
            } catch(Exception ex) {
                LOGGER.error(String.format("Failed to register %s as %s", dnsEntry.getFqdn(), dnsEntry.getIpAddress()), ex);
            }
        });
    } 
}
