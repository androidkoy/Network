package koyrim.util.network.nwtservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class UDPEngine extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.startService(new Intent(this, UDPService.class));
		Toast.makeText(this, "UDP Engine Start", Toast.LENGTH_SHORT).show();
		finish();

	}

}
