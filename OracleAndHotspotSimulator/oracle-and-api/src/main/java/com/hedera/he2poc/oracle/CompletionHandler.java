package com.hedera.he2poc.oracle;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CompletionHandler implements Runnable {
    @Override
    public void run() {
       log.info("Completion Handler invoked");
    }
}
