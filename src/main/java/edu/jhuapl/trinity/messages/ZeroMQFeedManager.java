package edu.jhuapl.trinity.messages;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
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
 * #L%
 */

import edu.jhuapl.trinity.messages.ZeroMQSubscriberConfig.CONNECTION;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asynchronous service that will subscribe to a ZeroMQ feed
 *
 * @author Sean Phillips
 */
public class ZeroMQFeedManager extends ScheduledThreadPoolExecutor {
    /**
     * Provides interface to route messages into GUI events
     */
    private MessageProcessor processor;
    /**
     * Configuration of the connection
     */
    private ZeroMQSubscriberConfig config;
    //default number of Detection objects to hold in the queue
    public static int DEFAULT_QUEUE_LIMIT = 1000;
    private int queueLimit = DEFAULT_QUEUE_LIMIT;
    /**
     * thread safe collection of BoundingBox objects
     */
    private ConcurrentLinkedQueue<String> concurrentMessageQueue;
    /**
     * When in playback mode limit how many detection data blocks to process
     */
    public boolean queueLimitEnabled = true;
    /**
     * how often to wake up to poll the queue
     */
    public static long DEFAULT_TIMER_RATE_MS = 50;
    /**
     * Variable used to scheduleAtFixedRate()
     */
    public long scheduledFixedRate = DEFAULT_TIMER_RATE_MS;
    /**
     * starts/stops the scheduled thread from updating the view
     */
    public boolean enabled = false;

    private boolean connected = false;
    private String currentHost = null;
    private Socket subscriber;
    private int threadGeneration = 0;

    public ZeroMQFeedManager(int corePoolSize,
                             ZeroMQSubscriberConfig config, MessageProcessor processor) {
        super(corePoolSize);
        this.config = config;
        this.processor = processor;
        concurrentMessageQueue = new ConcurrentLinkedQueue<>();
        setThreadFactory((Runnable r) -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("ZeroMQ Feed Manager processing thread " + threadGeneration);
            return t;
        });
    }

    public void startProcessing() {
        enabled = true;
        scheduledFixedRate = config.updateRateMS;
        if (null != subscriber) {
            disconnect(false);
        }
        System.out.println("Creating ZeroMQ subscriber at " + config.host);
        try (ZContext context = new ZContext()) {
            if (config.connection == CONNECTION.SUBSCRIBER) {
                subscriber = context.createSocket(SocketType.SUB);
                subscriber.connect(config.host);
                subscriber.subscribe(""); //must subscribe with at least the empty option
            } else {
                subscriber = context.createSocket(SocketType.PULL);
                subscriber.bind(config.host);
            }

            currentHost = config.host;
            connected = true;
            System.out.println("Starting ZeroMQ subscriber scheduled thread " + threadGeneration);
            this.scheduleAtFixedRate(() -> {
                if (enabled) {
                    try {
                        processQueue();
                    } catch (IOException ex) {
                        Logger.getLogger(ZeroMQFeedManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }, 0, scheduledFixedRate, TimeUnit.MILLISECONDS);
            threadGeneration++;
            while (connected && !Thread.currentThread().isInterrupted()) {
                if (enabled) {
                    int wdt = 0; //watch dog timer
                    String contents = ""; //if this is ever null then the zmq buffer is empty
                    //Poll messages from ZMQ buffer up to a limit of 1000
                    while (null != contents && wdt < 5000) {
                        contents = null; //reset contents to null prior to ZMQ recvStr
                        //@DEBUG SMP Useful debugging print
                        //System.out.println("Attempting to recvStr from ZeroMQ...");
                        // Read single message contents.
                        contents = subscriber.recvStr(ZMQ.DONTWAIT); //Null on no message
                        if (null != contents) {
                            //@DEBUG SMP Useful debugging print
                            //System.out.println("Recieved: " + contents);
                            concurrentMessageQueue.add(contents);
                            trimQueueNow();
                        } else
                            break; //null from recvStr means nothing in the zmq buffer
                        wdt++; //keep ticking watch dog timer
                    }
                }
                Thread.sleep(scheduledFixedRate);
            }
            System.out.println("Ending ZeroMQ subscriber scheduled thread " + threadGeneration);
        } catch (Exception ex) {
            Logger.getLogger(ZeroMQFeedManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            disconnect(false);
        }
    }

    public void setEnableProcessing(boolean enabled) {
        this.enabled = enabled;
    }

    public void setConfig(ZeroMQSubscriberConfig config) {
        this.config = config;
    }

    private void processQueue() throws IOException {
//        System.out.print("Setting took.... ");
//        long startTime = System.nanoTime();
        //retrieve all the messages from the queue
        String[] messages = concurrentMessageQueue.toArray(new String[0]);
        concurrentMessageQueue.clear();
        for (String message : messages) {
            processor.process(message);
        }
//        Utils.printTotalTime(startTime);
    }

    public void trimQueueNow() {
//        long startTime;
//        System.out.print("Queueing took... ");
//        startTime = System.nanoTime();
        int size = concurrentMessageQueue.size();
        if (queueLimitEnabled && size > getQueueLimit()) {
            for (int i = 0; i < size - getQueueLimit(); i++)
                concurrentMessageQueue.poll();
        }
//        Utils.printTotalTime(startTime);
    }

    public void disconnect(boolean shutdown) {
        connected = false;
        if (null != subscriber) {
            if (config.connection == CONNECTION.SUBSCRIBER) {
                subscriber.unsubscribe("");
                subscriber.disconnect(currentHost);
            } else {
                subscriber.unbind(config.host);
            }
            subscriber.close();
        }
        if (shutdown) {
            boolean status = shutdownAndAwaitTermination(this, 500, TimeUnit.MILLISECONDS);
            System.out.println("Thread Termination status: " + status);
        }
    }

    /**
     * Static Helper to shutdown async executor services. This allows a class
     * executor to be restarted later.
     *
     * @param service
     * @param timeout
     * @param unit
     * @return
     */
    public static boolean shutdownAndAwaitTermination(
        ExecutorService service, long timeout, TimeUnit unit) {
        long halfTimeoutNanos = unit.toNanos(timeout) / 2;
        // Disable new tasks from being submitted
        service.shutdown();
        try {
            // Wait for half the duration of the timeout for existing tasks to terminate
            if (!service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS)) {
                // Cancel currently executing tasks
                service.shutdownNow();
                // Wait the other half of the timeout for tasks to respond to being cancelled
                service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException ie) {
            // Preserve interrupt status
            Thread.currentThread().interrupt();
            // (Re-)Cancel if current thread also interrupted
            service.shutdownNow();
        }
        return service.isTerminated();
    }

    /**
     * @return the queueLimit
     */
    public int getQueueLimit() {
        return queueLimit;
    }

    /**
     * @param queueLimit the queueLimit to set
     */
    public void setQueueLimit(int queueLimit) {
        this.queueLimit = queueLimit;
    }

    public ZeroMQSubscriberConfig getConfig() {
        return config;
    }

    public boolean isFeed(String feedName) {
        return config.name.contentEquals(feedName);
    }

}
