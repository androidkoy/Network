package koyrim.util.network.nwtservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

public class AutoStart extends BroadcastReceiver {

	public static final String ACTION_RESTART_SERVICE = "AutoStart.Restart";
	private UDPService svcContext = ((UDPService) UDPService.svcContext);

	@Override
	public void onReceive(Context context, Intent mainIntent) {

		String action = mainIntent.getAction().toString();

		if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			Intent intent = new Intent(context, UDPService.class);
			context.startService(intent);

			Toast.makeText(context, "mode : " + action, Toast.LENGTH_SHORT)
					.show();
			Log.i("Autostart", "started");
		}

		else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			ConnectivityManager conManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetInfo = conManager.getActiveNetworkInfo();
			// Log.e("activeNetInfo",activeNetInfo.getTypeName()+"");

			try {

				if (activeNetInfo.getTypeName() != null) {
					if (activeNetInfo.getTypeName().equals("WIFI")) {
						if (isWifi())
							svcContext.AddStory("WiFi On");
					} else {
						if (isMobile(conManager))
							svcContext.AddStory("Mobile On");
					}
				} else {
					svcContext.AddStory("Network off");
				}
			} catch (Exception e) {
				svcContext.AddStory("Network Off");
			}
		}

	}

	// wifi 사용가능
	public boolean isWifi() {
		boolean result = false;
		WifiManager wm;
		wm = (WifiManager) svcContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wm.getConnectionInfo();

		if (wInfo != null) {
			// 연결상태 확인
			DetailedState ni_ds = WifiInfo.getDetailedStateOf(wInfo
					.getSupplicantState());
			if ((wInfo.getIpAddress() > 0 && wInfo.getSSID() != null && wInfo
					.getSupplicantState().toString().equals("COMPLETED"))
					&& (ni_ds == DetailedState.CONNECTED || ni_ds == DetailedState.OBTAINING_IPADDR)) {
				// RSSI 는 -100에 가까울수록 안좋고 0에 가까울수록 좋음
				// Log.e("wifi rssi",wInfo.getRssi()+"");
				if (wInfo.getRssi() > -75)
					result = true;
			}
		}

		return result;
	}

	// 3g 사용가능
	public boolean isMobile(ConnectivityManager connec) {
		boolean result = false;
		NetworkInfo networkInfo;

		networkInfo = connec.getActiveNetworkInfo();
		if (networkInfo != null) {
			if (networkInfo.getType() == 0)
				result = true;
		}
		return result;
	}

}
