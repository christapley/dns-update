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
import com.hexagon.geospatial.infrastructure.dns.update.entity.DnsEntryARecord;
import com.hexagon.geospatial.infrastructure.dns.update.entity.DnsEntryCname;
import com.hexagon.geospatial.infrastructure.dns.update.storage.DnsEntryStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author Chris
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class DnsUpdateController {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(DnsUpdateController.class);
    
    @Autowired
    DnsEntryStorage dnsEntriesStorage;
    
    @Autowired
    DnsClient dnsClient;
    
    @Value("${dns.update.millis:600000}")
    long dnsUpdatePeriod;
    
    AtomicLong lastNewDnsEntryTime;
    long lastDnsUpdateTime;
    
    @PostConstruct
    public void postConstruct() {
        lastNewDnsEntryTime = new AtomicLong(Instant.now().toEpochMilli());
        lastDnsUpdateTime = Instant.now().toEpochMilli() - dnsUpdatePeriod;
    }
    
    @GetMapping("/register/{fqdn:.+}/{ipAddress:.+}")
    @ResponseBody
    public ResponseEntity<DnsEntryARecord> register(@PathVariable("fqdn") String fqdn,
            @PathVariable("ipAddress") String ipAddress) throws ResponseStatusException {
        
        LOGGER.info(String.format("Received request to register %s as %s", fqdn, ipAddress));
        try {
            DnsEntryARecord dnsEntry = new DnsEntryARecord(fqdn, ipAddress);
            dnsEntriesStorage.addDnsEntry(dnsEntry);
            lastNewDnsEntryTime.set(Instant.now().toEpochMilli());
            return ResponseEntity.ok().body(dnsEntry);
        } catch(IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Unable to store DnsEntryARecord(%s, %s)", fqdn, ipAddress), ex);
        }
    }
    
    @GetMapping("/register/cname/{fqdnExisting:.+}/{fqdnNew:.+}")
    @ResponseBody
    public ResponseEntity<DnsEntryCname> registerCname(@PathVariable("fqdnExisting") String fqdnExisting,
            @PathVariable("fqdnNew") String fqdnNew) throws ResponseStatusException {
        
        LOGGER.info(String.format("Received request to CNAME %s as %s", fqdnNew, fqdnExisting));
        try {
            DnsEntryCname dnsEntry = new DnsEntryCname(fqdnNew, fqdnExisting);
            dnsEntriesStorage.addDnsEntry(dnsEntry);
            lastNewDnsEntryTime.set(Instant.now().toEpochMilli());
            return ResponseEntity.ok().body(dnsEntry);
        } catch(IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Unable to store DnsEntryCname(%s, %s)", fqdnNew, fqdnExisting), ex);
        }
    }
    
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<List<DnsEntry>> listAll() throws ResponseStatusException {
        try {
            return ResponseEntity.ok().body(dnsEntriesStorage.listAllDnsEnrties());
        } catch(IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store list DnsEntries", ex);
        }
    }
       
    @Scheduled(fixedRate = 10000)
    public void updateDns() throws IOException, Exception {
        long loadedLastNewDnsEntryTime = lastNewDnsEntryTime.get();
        long now = Instant.now().toEpochMilli();
        if(loadedLastNewDnsEntryTime > lastDnsUpdateTime || now > lastDnsUpdateTime + dnsUpdatePeriod) {
            lastDnsUpdateTime = now;
            List<DnsEntry> currentDnsEntries = dnsEntriesStorage.listAllDnsEnrties();
            currentDnsEntries.stream().forEach((dnsEntry) -> {
                try {
                    if(dnsEntry instanceof DnsEntryARecord) {
                        dnsClient.UpdateARecordEntry((DnsEntryARecord)dnsEntry);    
                    } else if(dnsEntry instanceof DnsEntryCname) {
                        dnsClient.UpdateCnameRecordEntry((DnsEntryCname)dnsEntry);    
                    }
                } catch(Exception ex) {
                    LOGGER.error(String.format("Failed to register %s with %s", dnsEntry.getFqdn(), dnsEntry.toString()), ex);
                }
            });
        }
    } 
}
