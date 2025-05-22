package com.tally_backup.tally_backup.Services;

import com.tally_backup.tally_backup.Config.TallyConnectionConfig;
import com.tally_backup.tally_backup.Dto.TallyConnection;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TallyApiConnectionPool {
    public ConcurrentLinkedQueue<TallyConnection> tallyConnections;

    public TallyConnectionConfig config;
    public TallyApiConnectionPool(TallyConnectionConfig tallyConfig) {
        tallyConnections = new ConcurrentLinkedQueue<>();
        this.config = tallyConfig;
    }

    @PostConstruct
    public void init(){
        Arrays.asList(config.ports).forEach( x -> {
            tallyConnections.add(new TallyConnection(x));
        });
    }

    public synchronized TallyConnection getTallyConnection() throws InterruptedException {
        if (tallyConnections.isEmpty()) {
            tallyConnections.wait(600000);
            if (tallyConnections.isEmpty()) {
                throw new InterruptedException("Cannot acquire connection timeout");
            }
        }

        return tallyConnections.poll();
    }

    public synchronized void closeTallyConnection(TallyConnection tallyConnection) throws InterruptedException {
        if (!tallyConnections.contains(tallyConnection)) {
            tallyConnections.add(tallyConnection);
            Thread.sleep(200);
        }
        notifyAll();
    }
}