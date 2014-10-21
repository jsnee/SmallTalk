package com.example.smalltalk;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
//comment
//comment
public class MainActivity extends Activity {

	protected static int audioValue = 0;
	private ArrayList<QuestionCategory> questionCategories = new ArrayList<QuestionCategory>();
	public ArrayAdapter<QuestionCategory> categoryAdapter;
	public final static String EXTRA_CATEGORY = "com.example.smalltalk.CATEGORY";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		categoryAdapter = new ArrayAdapter<QuestionCategory>(this, android.R.layout.simple_list_item_1, questionCategories);
		loadQuestionCategories();
		ListView categoryListView = (ListView) findViewById(R.id.mainCategoryList);
		categoryListView.setAdapter(categoryAdapter);
		categoryListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String selectedCategory = ((TextView)view).getText().toString();
				if (QuestionCategory.isQuestionCategoryTitle(selectedCategory)) {
					//PUT ADD NEW CATEGORY CODE HERE
				} else {
					System.out.println(selectedCategory);
					initializeGameScreen(selectedCategory);
					//Intent intent = new Intent(this, GameScreen.class);
				}
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
		//String[] categories = getResources().getStringArray(R.array.categories);
		for (String eachCategory : getResources().getStringArray(R.array.categories)) {
			questionCategories.add(new QuestionCategory(eachCategory));
			System.out.println(eachCategory);
		}
		questionCategories.add(QuestionCategory.getRandomCategory());
		categoryAdapter.notifyDataSetChanged();
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
	            case  R.id.addQuestion:
	            	//+ Add Question selected
	            	
	                return true;	                
	            default:
	                return super.onOptionsItemSelected(item);

	        }
	     
	}
	
	protected void initializeGameScreen(String selectedCategory) {
		Intent intent = new Intent(this, GameScreen.class);
		intent.putExtra(EXTRA_CATEGORY, selectedCategory);
		startActivity(intent);
	}

}