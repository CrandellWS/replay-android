package io.replay.framework.network;

import android.util.Pair;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import io.replay.framework.BuildConfig;
import io.replay.framework.model.ReplayJsonObject;
import io.replay.framework.model.ReplayRequest;
import io.replay.framework.util.ReplayLogger;

/**The ReplayNetworkManager is a simple wrapper around the Android/Java {@link java.net.HttpURLConnection}.
 * Given a {@link io.replay.framework.model.ReplayRequest} object, this class will POST the JSON body to the
 * appropriate endpoint.
 *
 */
public class ReplayNetworkManager {

    /**
     * Post the request to the server. This operation occurs in the thread that called it.
     *
     * @param request The request to be sent.
     * @return a {@link android.util.Pair}, where the first parameter is an Integer representing
     * {@link java.net.HttpURLConnection#getResponseCode()}, and the
     * second being {@link java.net.HttpURLConnection#getResponseMessage()}
     */
    public static Pair<Integer, String> doPost(ReplayRequest request) throws IOException {
        ReplayJsonObject data = new ReplayJsonObject();
        ReplayJsonObject json = new ReplayJsonObject();
        json.put("replay_key", "20394281092");

        data.put("distinct_id", "a3098fjs2");
        data.put("event", "bar");
        data.put("session_id", "55b17871-fbc5-11e3-9727-3965d78f0c34");
        json.put("data",data);
        final byte[] jsonBody = json.toString().getBytes();

        //final byte[] jsonBody = request.getBytes();

        URL url = new URL(BuildConfig.REPLAY_URL + request.getType());
        ReplayLogger.d("","POSTing to ", url.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(jsonBody.length);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", Integer.toString(jsonBody.length));
        connection.setUseCaches(false);

        BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream());
        bos.write(jsonBody);
        bos.flush();
        bos.close();
        return Pair.create(connection.getResponseCode(), connection.getResponseMessage());
    }
}
