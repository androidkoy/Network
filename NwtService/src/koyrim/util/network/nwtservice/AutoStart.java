package koyrim.util.network.nwtservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AutoStart extends BroadcastReceiver {

	public static final String ACTION_RESTART_SERVICE = "AutoStart.Restart";

	@Override
	public void onReceive(Context arg0, Intent arg1) {
        //Intent intent = new Intent(arg0,UDPEngine.class);
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //arg0.startActivity(intent);
        Intent intent = new Intent(arg0,UDPService.class);
		arg0.startService(intent);
        String action = arg1.getAction().toString();
		Toast.makeText(arg0, "mode : "+action, Toast.LENGTH_SHORT).show();
        Log.i("Autostart", "started");

	}

}
