package edu.drake.smalltalk;

import java.util.List;

import com.example.smalltalk.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CategoryListAdapter extends BaseAdapter {
    private Activity activity;
    private LayoutInflater inflater;
    private List<QuestionCategory> categories;
    
    public CategoryListAdapter(Activity activity, List<QuestionCategory> categories) {
    	this.activity = activity;
    	this.categories = categories;
    }

	@Override
	public int getCount() {
		return categories.size();
	}

	@Override
	public QuestionCategory getItem(int position) {
		return categories.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
 
        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.category_listitem, null);

        TextView categoryTitle = (TextView) convertView.findViewById(R.id.categoryTitle);
 
        // getting category data for the row
        QuestionCategory questionCategory = categories.get(position);
         
        // title
        categoryTitle.setText(questionCategory.getCategoryTitle());
 
        return convertView;
    }

}
