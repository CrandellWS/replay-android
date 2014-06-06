package io.replay.framework;

public interface ReplayConfig {

	/** ReplayIO API key */
	public static final String KEY_REPLAY_KEY = "replayKey";
	public static final String KEY_CLIENT_ID = "clientId";
	public static final String KEY_SESSION_ID = "sessionId";
	public static final String KEY_DATA = "data";

	/** The server URL requests will be posted to */
	public static final String REPLAY_URL = "http://dyn.originatechina.com:3000/";

	public static final String REQUEST_TYPE_ALIAS = "alias";
	public static final String REQUEST_TYPE_EVENTS = "events";
	
	public static final String PREF_DISPATCH_INTERVAL = "dispatchInterval";
	public static final String PREF_ENABLED = "enabled";
	public static final String PREF_DEBUG_MODE_ENABLED = "debugMode";
}
