package koyrim.util.network.nwtservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.BigPictureStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class UDPService extends Service {

	final static String tag = "UDPService";

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onCreate() {
		unregisterRestartAlarm(); // �̹� ��ϵ� �˶��� ������ ����

		setInit();
	}

	public void onDestroy() {
		Toast.makeText(this, "UDPService Stop", Toast.LENGTH_SHORT).show();
		registerRestartAlarm(); // ���񽺰� ���� �� �˶� ���
		Log.d(tag, "onDestroy");
	}

	void registerRestartAlarm() {
		Log.d(tag, "registerRestartAlarm");
		Intent intent = new Intent(this, AutoStart.class);
		intent.setAction(AutoStart.ACTION_RESTART_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0); // ��ε��ɽ�Ʈ��
																				// Intent
		long firstTime = SystemClock.elapsedRealtime(); // ���� �ð�
		firstTime += 3 * 1000; // 10�� �Ŀ� �˶��̺�Ʈ �߻�
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE); // �˶�
																			// ����
																			// ���
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
				10 * 1000, sender); // �˶���
	}

	void unregisterRestartAlarm() {
		Log.d(tag, "unregisterRestartAlarm");
		Intent intent = new Intent(UDPService.this, AutoStart.class);
		intent.setAction(AutoStart.ACTION_RESTART_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(UDPService.this, 0,
				intent, 0);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(sender);
	}

	public int onStartCommand(Intent intent, int flags, int startid) {
		Intent intents = new Intent(getBaseContext(), UDPEngine.class);
		// startActivity(intents);
		Toast.makeText(this, "UDPService Start", Toast.LENGTH_SHORT).show();
		Log.d(tag, "onStart");

		return START_REDELIVER_INTENT;
	}

	String connectedIP;
	boolean connected;
	int serverPort = 10002;
	MessageBox sendBox, talkBox;

	private void setInit() {

		// getIPAddress();
		connectedIP = "";
		connected = false;

		// messagebox
		sendBox = new MessageBox();
		talkBox = new MessageBox();

		runUDPThread();
	}

	private String getIPAddress() {
		try {
			Socket socket = new Socket("www.google.com", 80);
			return socket.getLocalAddress().toString();
		} catch (Exception e) {
			Log.i("ERROR", e.getMessage());
		}
		return "127.0.0.2";
	}

	Thread thSendPkt;
	
	@SuppressLint("NewApi")
	private void sendPkt(String msg) {
		sendBox.set(msg);
		Log.i("sendPkt", msg);
		
		if (connectedIP.isEmpty())
			return;
		if(thSendPkt != null && !thSendPkt.getState().equals(Thread.State.TERMINATED))
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
			runUDPThread();
		} else if (param[0].equals("setIP")) {
			connectedIP = param[1];
		} else if (param[0].equals("disconn")) {
			connected = false;
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
				res +=  " \r\n" + line;
			}

			Log.i("ExeCmd", res);
			return res;
		} catch (Exception e) {
			e.fillInStackTrace();
			Log.e("ExeCmd", "Unable to execute command\r\n" + e.toString());
			return "Unable to execute command\r\n" + e.toString();
		}
	}

	private void runUDPThread() {
		connected = true;
		Thread udpThread = new Thread(new Runnable() {

			public void run() {
				try {
					String msg;
					DatagramSocket sck = new DatagramSocket(serverPort);

					AddStory("#] UDP Server Receiving at " + getIPAddress()
							+ " : " + serverPort + " ... ");
					while (connected) {
						byte[] buf = new byte[1024];
						DatagramPacket dPkt = new DatagramPacket(buf,
								buf.length);
						sck.receive(dPkt);
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
				} catch (Exception e) {
					// Log.i("ERROR", e.getMessage());
					AddStory("ERR : " + e.toString() + " / " + e.getMessage());
				}
			}
		});

		udpThread.start();

	}

	private Handler mHandler = new Handler() {
		@SuppressLint("NewApi")
		public void handleMessage(Message msg) {

			NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			Builder builder = new Builder(UDPService.this);
			builder.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle("�˸��ϴ�").setContentText(msg.obj.toString())
					.setAutoCancel(true)
					// �˸��ٿ��� �ڵ� ����
					// .setVibrate(new long[] { 1000, 1000 })
					.setSound(
							RingtoneManager
									.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
			// autoCancel : �ѹ� ������ �˸��ٿ��� �������.
			// vibrate : ����, �︮��, ����, �︮��... �и�����
			// ������ �Ƿ��� AndroidManifest.xml�� ���� ������ ��� �Ѵ�.

			Notification.BigTextStyle noti = new Notification.BigTextStyle(
					builder).setSummaryText("and More +")
					.setBigContentTitle("�˸��ϴ�.[Ȯ��]")
					.bigText(msg.obj.toString());

			// // �˶� Ŭ���� MainActivity�� ȭ�鿡 ����.
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

	private void AddStory(String talk) {
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
