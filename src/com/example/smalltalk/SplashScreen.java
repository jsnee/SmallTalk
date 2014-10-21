package com.example.smalltalk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashScreen extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.drawable.splashscreen);
		
		Thread logoTimer = new Thread() {
            public void run(){
                try{
                    int logoTimer = 0;
                    while(logoTimer < 3000){ //3000 = 3 sec
                        sleep(100);
                        logoTimer = logoTimer +100;
                    };
                    
            		//setContentView(R.layout.activity_main);
                    
                    //after 5 sec, it will take user to app's home screen
	            	startActivity(new Intent("com.example.homeScreen"));
                } 
                 
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                 
                finally{
                    finish();
                }
            }
        };
         
        logoTimer.start();
	    }
}
