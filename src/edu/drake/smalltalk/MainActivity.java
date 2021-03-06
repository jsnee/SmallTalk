package edu.drake.smalltalk;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
//comment
//comment
public class MainActivity extends Activity {

	protected static int audioValue = 0;
	private List<QuestionCategory> questionCategories = new ArrayList<QuestionCategory>();
	protected CategoryListAdapter categoryAdapter;
	public final static String EXTRA_CATEGORY = "com.example.smalltalk.CATEGORY";
	public static AlertDialog loadingDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		SettingsSingleton.loadSettings(PreferenceManager.getDefaultSharedPreferences(this));
		loadQuestionCategories();
		categoryAdapter = new CategoryListAdapter(this, questionCategories);
		ImageView mainBackground = (ImageView) findViewById(R.id.mainBackground);
		mainBackground.setImageBitmap(BitmapUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.main_background, 100, 100));
		ListView categoryListView = (ListView) findViewById(R.id.mainCategoryList);
		categoryListView.setAdapter(categoryAdapter);
		categoryListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String selectedCategory = ((TextView) view.findViewById(R.id.categoryTitle)).getText().toString();
				initializeGameScreen(selectedCategory);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}
	
	public void loadQuestionCategories() {
		for (String eachCategory : getResources().getStringArray(R.array.categories)) {
			questionCategories.add(new QuestionCategory(eachCategory));
		}
		questionCategories.add(QuestionCategory.getRandomCategory());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item 
	     switch(item.getItemId())
	        {
	            case  R.id.settings:
	            	//Settings selected
	            	startActivity(new Intent(this, SettingScreen.class));
	                return true;                
	            default:
	                return super.onOptionsItemSelected(item);

	        }
	     
	}
	
	protected void initializeGameScreen(String selectedCategory) {
		Builder initDialogBuilder = new AlertDialog.Builder(this).setTitle("Loading").setMessage("Please wait while we load your questions...").setIcon(android.R.drawable.ic_popup_sync);
		loadingDialog = initDialogBuilder.create();
		loadingDialog.show();
		Intent intent = new Intent(this, GameScreen.class);
		GameScreen.preserveCategory = selectedCategory;
		startActivity(intent);
	}

}