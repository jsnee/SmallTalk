package edu.drake.smalltalk;

public class QuestionCategory {
	
	public String categoryName;
	public String categoryTitle;
	public static final String randomCategoryName = "RAND-CAT";
	public static final String randomCategoryTitle = "Randomize!";
	
	public QuestionCategory(String categoryName, String categoryTitle) {
		this.categoryName = categoryName;
		this.categoryTitle = categoryTitle;
	}

	public QuestionCategory(String categoryTitle) {
		this.categoryTitle = categoryTitle;
		if (categoryTitle.equals(randomCategoryTitle)) {
			this.categoryName = QuestionCategory.randomCategoryName;
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
	
	public static QuestionCategory getRandomCategory() {
		return new QuestionCategory(QuestionCategory.randomCategoryName, randomCategoryTitle);
	}
	
	public static boolean isQuestionCategoryTitle(String title) {
		boolean result = false;
		if (title.equals(QuestionCategory.getRandomCategory().getCategoryTitle())) {
			result = true;
		}
		return result;
	}
	
	public static boolean isQuestionCategoryName(String name) {
		boolean result = false;
		if (name.equals(randomCategoryName)) {
			result = true;
		}
		return result;
	}

}
