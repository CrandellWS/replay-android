package com.android.volley;

import java.util.concurrent.BlockingQueue;

import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Process;
import android.util.Log;

public class ReplayNetworkDispatcher extends Thread {

    /** The queue of requests to service. */
    private final BlockingQueue<Request<?>> mQueue;
    /** The network interface for processing requests. */
    private final Network mNetwork;
    /** The cache to write to. */
    private final Cache mCache;
    /** For posting responses and errors. */
    private final ResponseDelivery mDelivery;
    /** Used for telling us to die. */
    private volatile boolean mQuit = false;
    /** Used for telling how much time to wait till the next dispatch */
    private volatile int dispatchInterval = 0;
    /** Indicating if the dispatcher is processing the queue at the moment */
    private volatile boolean dispatching = false;
    
    
	public ReplayNetworkDispatcher(BlockingQueue<Request<?>> queue,
			Network network, Cache cache, ResponseDelivery delivery) {
        mQueue = queue;
        mNetwork = network;
        mCache = cache;
        mDelivery = delivery;
	}

    /**
     * Forces this dispatcher to quit immediately.  If any requests are still in
     * the queue, they are not guaranteed to be processed.
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void addTrafficStatsTag(Request<?> request) {
        // Tag the request (if API >= 14)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
        }
    }
    
	@Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Request<?> request;
        while (true) {
        	// check if the queue is empty for this dispatch, wait if empty
        	if (dispatching && mQueue.isEmpty()) {
        		//Log.d("REPLAY_IO", "mQueue isEmpty ");
        		dispatching = false;
        	}
        	
        	// sleep for a interval time or wait for the manual dispatch signal.
        	if (!dispatching && dispatchInterval >= 0) {
        		int delayed = 0;
        		while (delayed < dispatchInterval * 1000) {
        			// dispatchNow() is invoked
        			if (dispatching) {
        				//Log.d("REPLAY_IO", "dispatchNow() ?");
        				break;
        			}

        			try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
        			delayed += 100;
        		}
        		dispatching = true;
        		//Log.d("REPLAY_IO", "dispatching");
        	}
        	
            try {
                // Take a request from the queue.
                request = mQueue.take();
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    return;
                }
                continue;
            }

            try {
                request.addMarker("network-queue-take");

                // If the request was cancelled already, do not perform the
                // network request.
                if (request.isCanceled()) {
                    request.finish("network-discard-cancelled");
                    continue;
                }

                addTrafficStatsTag(request);

                // Perform the network request.
                NetworkResponse networkResponse = mNetwork.performRequest(request);
                request.addMarker("network-http-complete");

                // If the server returned 304 AND we delivered a response already,
                // we're done -- don't deliver a second identical response.
                if (networkResponse.notModified && request.hasHadResponseDelivered()) {
                    request.finish("not-modified");
                    continue;
                }

                // Parse the response here on the worker thread.
                Response<?> response = request.parseNetworkResponse(networkResponse);
                request.addMarker("network-parse-complete");

                // Write to cache if applicable.
                // TODO: Only update cache metadata instead of entire record for 304s.
                if (request.shouldCache() && response.cacheEntry != null) {
                    mCache.put(request.getCacheKey(), response.cacheEntry);
                    request.addMarker("network-cache-written");
                }

                // Post the response back.
                request.markDelivered();
                mDelivery.postResponse(request, response);
            } catch (VolleyError volleyError) {
                parseAndDeliverNetworkError(request, volleyError);
            } catch (Exception e) {
                VolleyLog.e(e, "Unhandled exception %s", e.toString());
                mDelivery.postError(request, new VolleyError(e));
            }
        }
    }

    private void parseAndDeliverNetworkError(Request<?> request, VolleyError error) {
        error = request.parseNetworkError(error);
        mDelivery.postError(request, error);
    }
    
    public void setDispatchInterval(int seconds) {
    	dispatchInterval = seconds;
    }
    
    public void dispatchNow() {
    	dispatching = true;
    }
    
}
