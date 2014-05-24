package koyrim.util.network.nwttoast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity implements OnEditorActionListener {

	TextView txtIP, txtTalk, txtStory;
	ScrollView scrollView;
	String connectedIP;
	boolean connected;
	int serverPort = 10002;
	MessageBox sendBox, talkBox;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setInit();
	}

	private void setInit() {
		// set ip address
		txtIP = (TextView) findViewById(R.id.txtIP);
		// getIPAddress();
		connectedIP = "";
		connected = false;

		// get resource
		txtTalk = (TextView) findViewById(R.id.talk);
		txtStory = (TextView) findViewById(R.id.Story);

		// set talk action
		txtTalk.setOnEditorActionListener(this);
		scrollView = (ScrollView) findViewById(R.id.scrStory);

		// messagebox
		sendBox = new MessageBox();
		talkBox = new MessageBox();
	}

	private void getIPAddress() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							Socket socket = new Socket("www.google.com", 80);
							txtIP.setText(socket.getLocalAddress().toString());
						} catch (Exception e) {
							// Log.i("ERROR", e.getMessage());
							AddStory("ERR : " + e.toString() + " / "
									+ e.getMessage());
						}
					}
				});
			}
		}).start();
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE
				|| (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
			String msg = txtTalk.getText().toString();
			if (msg.length() < 1)
				return false;
			AddStory(msg);

			if (msg.substring(0, 1).equals("/")) {
				sendCmd(msg.substring(1));
			} else if (connectedIP.length() > 7) {
				sendPkt(msg);
			}

			txtTalk.setText("");

		}
		return false;
	}

	private void sendPkt(String msg) {
		sendBox.set(msg);
		new Thread(new Runnable() {

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
					// Log.i("ERROR", e.getMessage());
					AddStory("ERR : " + e.toString() + " / " + e.getMessage());
				}

			}
		}).start();
	}

	private void sendCmd(String cmd) {
		String[] param = cmd.split(" ");

		if (param[0].equals("conn")) {
			runUDPThread();
		} else if (param[0].equals("setIP")) {
			connectedIP = param[1];
		} else if (param[0].equals("disconn")) {
			connected = false;
		}

	}

	private void runUDPThread() {
		connected = true;
		Thread udpThread = new Thread(new Runnable() {

			public void run() {
				try {
					String msg;
					DatagramSocket sck = new DatagramSocket(serverPort);

					AddStory("#] UDP Server Receiving ... ");
					while (connected) {
						byte[] buf = new byte[1024];
						DatagramPacket dPkt = new DatagramPacket(buf,
								buf.length);
						sck.receive(dPkt);
						msg = new String(dPkt.getData(), 0, dPkt.getLength());
						AddStory("R] " + dPkt.getAddress().toString() + " : "
								+ msg);
					}
				} catch (Exception e) {
					// Log.i("ERROR", e.getMessage());
					AddStory("ERR : " + e.toString() + " / " + e.getMessage());
				}
			}
		});

		udpThread.start();

	}

	private void AddStory(String talk) {
		talkBox.set(talk);

		txtStory.post(new Runnable() {

			@Override
			public void run() {
				while (!talkBox.isEmpty()) {
					txtStory.setText(txtStory.getText() + "\n" + talkBox.get());
					scrollView.postDelayed(new Runnable() {
						public void run() {
							scrollView.smoothScrollBy(0, txtStory.getHeight());
						}
					}, 100);
				}

			}
		});

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
