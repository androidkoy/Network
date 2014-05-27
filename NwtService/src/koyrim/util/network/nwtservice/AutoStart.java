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
		Log.i("onReceive", action + " // " + mainIntent.getDataString());

		if (!UDPService.isRestarting && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

			UDPService.isRestarting = true;
			
			ConnectivityManager conManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetInfo = conManager.getActiveNetworkInfo();

			if( activeNetInfo == null) {
				//svcContext.stopServer();
				svcContext.AddStory("Network Off");
				UDPService.connMode = -1;
				UDPService.isRestarting = false;
				return;
			}
			if( UDPService.connMode == activeNetInfo.getType() ){
				UDPService.isRestarting = false;
				return;
			}
			try {

				svcContext.stopServer(); 

				new Thread(new Runnable() {
					@Override
					public void run() {
						int timeout = 5;
						while (svcContext.isServerAlive()) {
							try {
								Log.i("isServerAlive()", svcContext
										.isServerAlive().toString());
								Thread.sleep(1000);
								if (--timeout < 0)
									break;
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if (svcContext.startServer())
							UDPService.isRestarting = false;
						if (!UDPService.isRestarting) {
							UDPService.connMode  = activeNetInfo.getType();
							if (UDPService.connMode == ConnectivityManager.TYPE_WIFI) {
								svcContext.AddStory("WiFi On");
							} else if (UDPService.connMode == ConnectivityManager.TYPE_MOBILE) {
								svcContext.AddStory("Mobile On");
							}
						}
						else {
							svcContext.stopServer();
							svcContext.AddStory("Network Off");
							UDPService.connMode = -1;
						}
						UDPService.isRestarting = false;
							

					}
				}).start();

			} catch (Exception e) {
				svcContext.stopServer();
				svcContext.AddStory("Network Off");
				UDPService.connMode = -1;
				UDPService.isRestarting = false;
			}
		} else if (action.equals(Intent.ACTION_PACKAGE_ADDED)
				|| action.equals(Intent.ACTION_PACKAGE_REPLACED)
				|| action.equals(ACTION_RESTART_SERVICE)
				|| action.equals(Intent.ACTION_BOOT_COMPLETED)) {

			Intent intent = new Intent(context, UDPService.class);
			context.startService(intent);

			Toast.makeText(context, "mode : " + action, Toast.LENGTH_SHORT)
					.show();
			Log.i("Autostart", "started");
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
