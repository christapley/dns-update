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
package com.hexagon.geospatial.infrastructure.dns.update.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hexagon.geospatial.infrastructure.dns.update.entity.DnsEntry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Chris
 */
@Service
public class JsonFileDnsEntryStorage implements DnsEntryStorage {

    @Value("${json.storage.file:dnsEntries.json}")
    File jsonStorageFile;
    
    long jsonStorageFileLastModified;
    Map<String, DnsEntry> dnsEntries;
    ObjectMapper mapper;
  
    public JsonFileDnsEntryStorage() {
        jsonStorageFileLastModified = -1;
        dnsEntries = new HashMap<>();
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    }
    
    public void readDnsEntriesFromFile() throws IOException {
        dnsEntries.clear();
        if(jsonStorageFile.exists()) {
            JsonFileDnsEntryEntity entity = mapper.readValue(jsonStorageFile, JsonFileDnsEntryEntity.class);
            entity.getDnsEntries().stream().forEach((dnsEntry) -> {
                dnsEntries.put(dnsEntry.getFqdn(), dnsEntry);
            });
        }
    }
    
    public List<DnsEntry> getDnsEnrtiesAsList() {
        List<DnsEntry> dnsEntriesList = new ArrayList<>();
        dnsEntries.entrySet().stream().forEach((entry) -> {
            dnsEntriesList.add(entry.getValue());
        });
        return dnsEntriesList;
    }
    
    public void writeDnsEntriesToFile() throws IOException {
        JsonFileDnsEntryEntity entity = new JsonFileDnsEntryEntity();
        entity.setDnsEntries(getDnsEnrtiesAsList());
        mapper.writeValue(jsonStorageFile, entity);
    }
    
    @Override
    public List<DnsEntry> listAllDnsEnrties() throws IOException {
        if(jsonStorageFileLastModified != jsonStorageFile.lastModified()) {
            readDnsEntriesFromFile();
        }
        return getDnsEnrtiesAsList();
    }

    @Override
    public void addDnsEntry(DnsEntry dnsEntry) throws IOException {
        if(jsonStorageFileLastModified != jsonStorageFile.lastModified()) {
            readDnsEntriesFromFile();
        }
        dnsEntries.put(dnsEntry.getFqdn(), dnsEntry);
        writeDnsEntriesToFile();
    }
}
