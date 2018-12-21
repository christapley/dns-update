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
package com.hexagon.geospatial.infrastructure.dns.update.client;

import com.hexagon.geospatial.infrastructure.dns.update.entity.DnsEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Chris
 */
@Service
public class NsUpdateDnsClient implements DnsClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(NsUpdateDnsClient.class);
    
    @Value("${nsupdate.bin:nsupdate}")
    String nsupdateCommand;
    
    @Value("${nsupdate.authoritive.server.ip}")
    String authServer;
    
    public void runCommand(String inputString) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        InputStream input = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
        
        try {
            DefaultExecutor executor = new DefaultExecutor();
            ExecuteStreamHandler streams = new PumpStreamHandler(output, error, input);
            executor.setStreamHandler(streams);
            executor.setExitValue(0);
            CommandLine commandLine = new CommandLine(nsupdateCommand);
            executor.execute(commandLine);
            LOGGER.debug(String.format("Output '%s'", output.toString()));
        } catch(Exception ex) {
            throw new IllegalStateException(String.format("Output '%s'. Error: '%s'", output.toString(), error.toString()), ex);
        }
    }
    
    @Override
    public void UpdateARecordEntry(DnsEntry dnsEntry) throws Exception {
        
        LOGGER.info("Updating %s as %s", dnsEntry.getFqdn(), dnsEntry.getIpAddress());
        
        InetAddress ipAddress = InetAddress.getByName(dnsEntry.getIpAddress());
        byte[] address = ipAddress.getAddress();
        
        String ptrUpdate = String.format(
                "server %s\n" + 
                "update delete %s. A\n" +
                "update add %s. 86400 A %d.%d.%d.%d\n" +
                ";show\n" + 
                "send",
        authServer, 
        dnsEntry.getFqdn(),
        dnsEntry.getFqdn(), Byte.toUnsignedInt(address[0]), Byte.toUnsignedInt(address[1]), Byte.toUnsignedInt(address[2]), Byte.toUnsignedInt(address[3]));
        
        runCommand(ptrUpdate);
        
        String ptrUpdateRr = String.format(
                "server %s\n" + 
                "update add %d.%d.%d.%d.in-addr.arpa 86400 PTR %s.\n" +
                ";show\n" + 
                "send",
        authServer, 
        Byte.toUnsignedInt(address[3]), Byte.toUnsignedInt(address[2]), Byte.toUnsignedInt(address[1]), Byte.toUnsignedInt(address[0]), dnsEntry.getFqdn());
        
        runCommand(ptrUpdateRr);
        
        LOGGER.info("Updated %s as %s successfully", dnsEntry.getFqdn(), dnsEntry.getIpAddress());
    }
}
