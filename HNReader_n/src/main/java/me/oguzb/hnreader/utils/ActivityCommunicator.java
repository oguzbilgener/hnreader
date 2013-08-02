package me.oguzb.hnreader.utils;

import android.os.Bundle;

public interface ActivityCommunicator {
	public void sendMessage(int message);
    public void sendBundle(Bundle bundle);
}
