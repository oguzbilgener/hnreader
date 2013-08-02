package me.oguzb.hnreader.utils;

import android.os.Bundle;

public interface FragmentCommunicator {
	public void passDataToFragment(int command);
    public void sendBundle(Bundle bundle);
}
