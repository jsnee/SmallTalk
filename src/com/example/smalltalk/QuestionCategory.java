package com.example.smalltalk;

public class QuestionCategory {
	
	public String categoryName;
	public String categoryTitle;
	public static final String addNewCategoryName = "ADD-NEW-CAT";
	
	public QuestionCategory(String categoryName, String categoryTitle) {
		this.categoryName = categoryName;
		this.categoryTitle = categoryTitle;
	}

	public QuestionCategory(String categoryTitle) {
		this.categoryTitle = categoryTitle;
		if (categoryTitle.equals("+ Add Your Own")) {
			this.categoryName = QuestionCategory.addNewCategoryName;
		} else {
			this.categoryName = categoryTitle.replaceAll(" ", "_");
		}
	}
	
	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryTitle() {
		return categoryTitle;
	}

	public void setCategoryTitle(String categoryTitle) {
		this.categoryTitle = categoryTitle;
	}
	
	@Override
	public String toString() {
		return this.getCategoryTitle();
	}
	
	public char[] categoryTitleAsArray() {
		return this.getCategoryTitle().toCharArray();
	}
	
	public static QuestionCategory getAddYourOwnCategory() {
		return new QuestionCategory(QuestionCategory.addNewCategoryName, "+ Add Your Own");
	}
	
	public static boolean isQuestionCategoryTitle(String title) {
		boolean result = false;
		if (title.equals(QuestionCategory.getAddYourOwnCategory().getCategoryTitle())) {
			result = true;
		}
		return result;
	}

}
