package in.ceeq.msdcs.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.pdfjet.A4;
import com.pdfjet.Align;
import com.pdfjet.Cell;
import com.pdfjet.Color;
import com.pdfjet.CoreFont;
import com.pdfjet.Font;
import com.pdfjet.PDF;
import com.pdfjet.Page;
import com.pdfjet.Table;
import com.pdfjet.TextLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVWriter;
import in.ceeq.msdcs.R;
import in.ceeq.msdcs.fragment.ExportFragment;
import in.ceeq.msdcs.provider.BaseColumns;
import in.ceeq.msdcs.provider.SurveyContract;
import in.ceeq.msdcs.provider.entity.User;

public class Utils {

    public static final String PIPE = " | ";

	/**
	 * Get formatted column name.
	 * 
	 * @param columnName
	 * @return formatted column name
	 */
	public static final String getColumnName(String columnName) {
		return columnName.replace("_", " ").toUpperCase(Locale.getDefault());
	}

	/**
	 * Get crop stage text.
	 * 
	 * @param context
	 * @param index
	 *            spinner index
	 * @return crop stage
	 */
	public static String getCropStageString(Context context, int index) {
		String[] cropStages = context.getResources().getStringArray(R.array.map_crop_stages);
		return cropStages[index];
	}

	/**
	 * Export data to a file.
	 * 
	 * @param fileType
	 *            type of file (i.e.. PDF, EXCEL etc.)
	 * @param context
	 *            context
	 * @param fileName
	 *            name of file
	 * @param data
	 *            data to be exported
	 */
	public static void export(int fileType, Context context, String fileName, Cursor data) {
		switch (fileType) {
		case ExportFragment.EXPORT_PDF:
			exportPdf(context, fileName, data);
			break;
		case ExportFragment.EXPORT_EXCEL:
			exportCsv(context, fileName, data);
			break;
		case ExportFragment.EXPORT_TEXT:
			exportText(context, fileName, data);
			break;
		default:
			break;
		}
	}

	private static String[] getDefaultExportHeaderNames() {
		return new String[] { SurveyContract.Details._ID, SurveyContract.Details.DATE_SOWING,
				SurveyContract.Details.DATE_SURVEY, SurveyContract.Details.LATITUDE, SurveyContract.Details.LONGITUDE,
				SurveyContract.Details.CROP_STAGE, SurveyContract.Details.DISEASE_NAME,
				SurveyContract.Details.DISEASE_SEVERITY_SCORE, SurveyContract.Details.PEST_NAME,
				SurveyContract.Details.PEST_INFESTATION_COUNT };
	}

	public static String getFormattedDate(long milis, String format) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(milis);
		return new SimpleDateFormat(format, Locale.getDefault()).format(calendar.getTime());
	}

	private static void exportPdf(Context context, String fileName, Cursor data) {
		try {
			File file = createFile(ExportFragment.EXPORT_PDF, APP_PATH, fileName, context);
			PDF pdf = new PDF(new FileOutputStream(file));
			Page page = new Page(pdf, A4.PORTRAIT);

			Font titleFont = new Font(pdf, CoreFont.COURIER_BOLD);
			titleFont.setSize(16.0f);
			Font textFont = new Font(pdf, CoreFont.HELVETICA);
			textFont.setSize(10.0f);
			TextLine title = new TextLine(titleFont, SurveyContract.Surveys.PATH.toUpperCase(Locale.getDefault()));
			title.setFont(titleFont);
			title.setColor(Color.black);
			title.setPosition(page.getWidth() / 2 - title.getWidth() / 2, 40f);
			title.drawOn(page);

			Table table = new Table();
			List<List<Cell>> tableData = new ArrayList<List<Cell>>();

			List<Cell> headerRow = new ArrayList<Cell>();
			titleFont.setSize(12.0f);
			for (String columnName : getDefaultExportHeaderNames()) {
				headerRow.add(new Cell(titleFont, getColumnName(columnName)));
			}

			for (Cell cell : headerRow) {
				cell.setTextAlignment(Align.CENTER);
				cell.setTopPadding(6.0f);
				cell.setRightPadding(6.0f);
				cell.setLeftPadding(6.0f);
				cell.setBottomPadding(6.0f);
			}

			tableData.add(headerRow);

			int idIndex = data.getColumnIndex(SurveyContract.Details._ID);
			int dateSowingIndex = data.getColumnIndex(SurveyContract.Details.DATE_SOWING);
			int dateSurveyIndex = data.getColumnIndex(SurveyContract.Details.DATE_SURVEY);
			int latitudeIndex = data.getColumnIndex(SurveyContract.Details.LATITUDE);
			int longitudeIndex = data.getColumnIndex(SurveyContract.Details.LONGITUDE);
			int cropStageIndex = data.getColumnIndex(SurveyContract.Details.CROP_STAGE);
			int diseaseNameIndex = data.getColumnIndex(SurveyContract.Details.DISEASE_NAME);
			int diseaseSeverityIndex = data.getColumnIndex(SurveyContract.Details.DISEASE_SEVERITY_SCORE);
			int pestNameIndex = data.getColumnIndex(SurveyContract.Details.PEST_NAME);
			int pestInfestationIndex = data.getColumnIndex(SurveyContract.Details.PEST_INFESTATION_COUNT);

			while (data.moveToNext()) {
				String id = data.getInt(idIndex) + "";
				String dateOfSowing = getFormattedDate(data.getLong(dateSowingIndex), "dd-MM-yyyy");
				String dateOfSurvey = getFormattedDate(data.getLong(dateSurveyIndex), "dd-MM-yyyy");
				DecimalFormat decimalFormat = new DecimalFormat("##.0000");
				String latitude = decimalFormat.format(data.getDouble(latitudeIndex));
				String longitude = decimalFormat.format(data.getDouble(longitudeIndex));
				String cropStage = getCropStageString(context, data.getInt(cropStageIndex));
				String diseaseName = data.getString(diseaseNameIndex);
				String diseaseSeverityScore = data.getString(diseaseSeverityIndex);
				String pestName = data.getString(pestNameIndex);
				String pestCount = data.getString(pestInfestationIndex);

				List<Cell> dataRow = new ArrayList<Cell>();

				dataRow.add(new Cell(textFont, id));
				dataRow.add(new Cell(textFont, dateOfSowing));
				dataRow.add(new Cell(textFont, dateOfSurvey));
				dataRow.add(new Cell(textFont, latitude));
				dataRow.add(new Cell(textFont, longitude));
				dataRow.add(new Cell(textFont, cropStage));
				dataRow.add(new Cell(textFont, diseaseName));
				dataRow.add(new Cell(textFont, diseaseSeverityScore));
				dataRow.add(new Cell(textFont, pestName));
				dataRow.add(new Cell(textFont, pestCount));

				for (Cell cell : dataRow) {
					cell.setTextAlignment(Align.CENTER);
					cell.setFont(textFont);
					cell.setPenColor(Color.black);
				}
				tableData.add(dataRow);
			}

			table.setData(tableData, Table.DATA_HAS_1_HEADER_ROWS);
			table.wrapAroundCellText();
			table.autoAdjustColumnWidths();

			int numOfPages = table.getNumberOfPages(page);
			int currentPage = 0;
			while (true) {
				TextLine pageNumber = new TextLine(textFont, ++currentPage + " of " + numOfPages);
				pageNumber.setPosition(pageNumber.getWidth() + 30.0f, page.getHeight() - 20.0f);
				pageNumber.drawOn(page);

				table.setLocation(page.getWidth() / 2 - table.getWidth() / 2, 60f);
				table.drawOn(page);
				if (!table.hasMoreData()) {
					table.resetRenderedPagesCount();
					break;
				}
				page = new Page(pdf, A4.PORTRAIT);
			}

			pdf.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			data.close();
		}

	}

	private static void exportCsv(Context context, String fileName, Cursor data) {
		int idIndex = data.getColumnIndex(SurveyContract.Details._ID);
		int dateSowingIndex = data.getColumnIndex(SurveyContract.Details.DATE_SOWING);
		int dateSurveyIndex = data.getColumnIndex(SurveyContract.Details.DATE_SURVEY);
		int latitudeIndex = data.getColumnIndex(SurveyContract.Details.LATITUDE);
		int longitudeIndex = data.getColumnIndex(SurveyContract.Details.LONGITUDE);
		int cropStageIndex = data.getColumnIndex(SurveyContract.Details.CROP_STAGE);
		int diseaseNameIndex = data.getColumnIndex(SurveyContract.Details.DISEASE_NAME);
		int diseaseSeverityIndex = data.getColumnIndex(SurveyContract.Details.DISEASE_SEVERITY_SCORE);
		int pestNameIndex = data.getColumnIndex(SurveyContract.Details.PEST_NAME);
		int pestInfestationIndex = data.getColumnIndex(SurveyContract.Details.PEST_INFESTATION_COUNT);

		try {
			File file = createFile(ExportFragment.EXPORT_EXCEL, APP_PATH, fileName, context);
			CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
			StringBuilder exportHeaderBuilder = new StringBuilder();
			for (String columnName : getDefaultExportHeaderNames()) {
				exportHeaderBuilder.append(getColumnName(columnName)).append(BaseColumns.COMMA.trim());
			}

			csvWrite.writeNext(exportHeaderBuilder.substring(0, exportHeaderBuilder.length() - 1).split(","));

			while (data.moveToNext()) {
				String id = data.getInt(idIndex) + "";
				String dateOfSowing = getFormattedDate(data.getLong(dateSowingIndex), "dd-MM-yyyy");
				String dateOfSurvey = getFormattedDate(data.getLong(dateSurveyIndex), "dd-MM-yyyy");
				DecimalFormat decimalFormat = new DecimalFormat("##.0000");
				String latitude = decimalFormat.format(data.getDouble(latitudeIndex));
				String longitude = decimalFormat.format(data.getDouble(longitudeIndex));
				String cropStage = getCropStageString(context, data.getInt(cropStageIndex));
				String diseaseName = data.getString(diseaseNameIndex);
				String diseaseSeverityScore = data.getString(diseaseSeverityIndex);
				String pestName = data.getString(pestNameIndex);
				String pestCount = data.getString(pestInfestationIndex);
				csvWrite.writeNext(new String[] { id, dateOfSowing, dateOfSurvey, latitude, longitude, cropStage,
						diseaseName, diseaseSeverityScore, pestName, pestCount });
			}
			csvWrite.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			data.close();
		}
	}

	private static void exportText(Context context, String fileName, Cursor data) {
		int idIndex = data.getColumnIndex(SurveyContract.Details._ID);
		int dateSowingIndex = data.getColumnIndex(SurveyContract.Details.DATE_SOWING);
		int dateSurveyIndex = data.getColumnIndex(SurveyContract.Details.DATE_SURVEY);
		int latitudeIndex = data.getColumnIndex(SurveyContract.Details.LATITUDE);
		int longitudeIndex = data.getColumnIndex(SurveyContract.Details.LONGITUDE);
		int cropStageIndex = data.getColumnIndex(SurveyContract.Details.CROP_STAGE);
		int diseaseNameIndex = data.getColumnIndex(SurveyContract.Details.DISEASE_NAME);
		int diseaseSeverityIndex = data.getColumnIndex(SurveyContract.Details.DISEASE_SEVERITY_SCORE);
		int pestNameIndex = data.getColumnIndex(SurveyContract.Details.PEST_NAME);
		int pestInfestationIndex = data.getColumnIndex(SurveyContract.Details.PEST_INFESTATION_COUNT);

		try {
			File file = createFile(ExportFragment.EXPORT_TEXT, APP_PATH, fileName, context);
			FileWriter writer = new FileWriter(file, true);
			StringBuilder exportHeaderBuilder = new StringBuilder();
			for (String columnName : getDefaultExportHeaderNames()) {
				exportHeaderBuilder.append(getColumnName(columnName)).append(PIPE);
			}
			writer.append(exportHeaderBuilder.substring(0, exportHeaderBuilder.length() - 1) + "\n\n");
			writer.flush();

			while (data.moveToNext()) {
				StringBuilder exportDataBuilder = new StringBuilder();
				exportDataBuilder.append(data.getInt(idIndex) + "").append(PIPE);
				exportDataBuilder.append(getFormattedDate(data.getLong(dateSowingIndex), "dd-MM-yyyy")).append(
                        PIPE);
				exportDataBuilder.append(getFormattedDate(data.getLong(dateSurveyIndex), "dd-MM-yyyy")).append(
                        PIPE);
				DecimalFormat decimalFormat = new DecimalFormat("##.0000");
				exportDataBuilder.append(decimalFormat.format(data.getDouble(latitudeIndex))).append(PIPE);
				exportDataBuilder.append(decimalFormat.format(data.getDouble(longitudeIndex)))
						.append(PIPE);
				exportDataBuilder.append(getCropStageString(context, data.getInt(cropStageIndex))).append(
                        PIPE);

                String diseaseName =data.getString(diseaseNameIndex);
                if(!TextUtils.isEmpty(diseaseName)) {
                    exportDataBuilder.append(diseaseName).append(PIPE);
                } else {
                    exportDataBuilder.append("").append(PIPE);
                }


                String diseaseSeverityScore = data.getString(diseaseSeverityIndex);
                if(!TextUtils.isEmpty(diseaseSeverityScore)) {
                    exportDataBuilder.append(diseaseSeverityScore).append(PIPE);
                } else {
                    exportDataBuilder.append("").append(PIPE);
                }

                String pestName = data.getString(pestNameIndex);
                if(!TextUtils.isEmpty(pestName)) {
                    exportDataBuilder.append(pestName).append(PIPE);
                } else {
                    exportDataBuilder.append("").append(PIPE);
                }

                String pestInfestationCount = data.getString(pestInfestationIndex);
                if(!TextUtils.isEmpty(pestInfestationCount)) {
                    exportDataBuilder.append(pestInfestationCount).append(PIPE);
                } else {
                    exportDataBuilder.append("").append(PIPE);
                }

				writer.append(exportDataBuilder.toString() + "\n");
				writer.flush();
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			data.close();
		}
	}

	/**
	 * Hide keyboard.
	 * 
	 * @param activity
	 */
	public static void hideKeyboard(Activity activity) {
		try {
			InputMethodManager inputManager = (InputMethodManager) activity
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**************************************
	 ***************** User ****************
	 **************************************/

	public static User getCurrentUser(Context context) {
		User user = new User();
		user.mId = Utils.getLongPrefs(context, CURRENT_USER_ID);
		user.mName = Utils.getStringPrefs(context, CURRENT_USER_NAME);
		user.mEmail = Utils.getStringPrefs(context, CURRENT_USER_EMAIL);

		return user;
	}

	/**************************************
	 ************** Provider ***************
	 **************************************/

	public static String getStringFromArray(String[] strings, String delimiter) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			sb.append(string).append(delimiter);
		}
		if (strings.length != 0)
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static Uri getTableContentUri(String mTableName) {
		return Uri.parse("content://" + SurveyContract.AUTHORITY + "/" + mTableName);
	}

	public static String getTableContentItemType(String mTableName) {
		return new StringBuilder(ContentResolver.CURSOR_ITEM_BASE_TYPE).append("/").append(mTableName).toString();
	}

	public static String getTableContentType(String mTableName) {
		return new StringBuilder(ContentResolver.CURSOR_DIR_BASE_TYPE).append("/").append(mTableName).toString();
	}

	public static String getJoinColumnName(String mTableName, String mColumnName) {
		return new StringBuilder(mTableName).append(".").append(mColumnName).toString();
	}

	public static String getColumnName(String mTableName, String mColumnName, String mAliasName) {
		return new StringBuilder(mTableName).append(".").append(mColumnName).append(" AS ").append(mAliasName)
				.toString();
	}

	/**************************************
	 ***************** Logs ***************
	 **************************************/

	/**
	 * Log debug message.
	 * 
	 * @param message
	 */
	public static void d(String message) {
		Log.d("@ceeq", message);
	}

	/**
	 * Log warning message.
	 * 
	 * @param message
	 */
	public static void w(String message) {
		Log.w("@ceeq", message);
	}

	/**
	 * Log informative message.
	 * 
	 * @param message
	 */
	public static void i(String message) {
		Log.i("@ceeq", message);
	}

	/**************************************
	 ***************** Phone ***************
	 **************************************/

	public static final int GPS = 1;

	public static final int INTERNET = 2;

	public static final int PLAY_SERVICES = 3;

	public static final int EXTERNAL_STORAGE = 4;

	/**
	 * 
	 * Check phone feature enabled
	 * 
	 * @param featureType
	 * @param context
	 * @return
	 */
	public static boolean enabled(int featureType, Context context) {
		boolean data = false;
		switch (featureType) {
		case GPS:
			data = isGpsEnabled(context);
			break;
		case INTERNET:
			data = isInternetEnabled(context);
			break;
		case PLAY_SERVICES:
			data = isPlayServiceInstalled(context);
			break;
		case EXTERNAL_STORAGE:
			data = isExternalStorageEnabled(context);
			break;
		default:
			data = false;
			break;

		}
		return data;
	}

	/**
	 * 
	 * Is GPS enabled
	 * 
	 * @param context
	 * 
	 * @return status
	 */
	private static boolean isGpsEnabled(Context context) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			return true;
		return false;
	}

	private static boolean isPlayServiceInstalled(Context context) {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		if (ConnectionResult.SUCCESS == resultCode)
			return true;
		return false;
	}

	private static boolean isExternalStorageEnabled(Context context) {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return mExternalStorageAvailable && mExternalStorageWriteable;
	}

	private static boolean isInternetEnabled(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	/**************************************
	 ************** Preferences ***********
	 **************************************/

	public static final String CURRENT_USER_ID = "current_user_id";

	public static final String CURRENT_USER_NAME = "current_user_name";

	public static final String CURRENT_USER_EMAIL = "current_user_email";

	public static final String IS_LOGGED_IN = "is_logged_in";

	public static Boolean getBooleanPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(key, false);
	}

	public static void setBooleanPrefs(Context ctx, String key, Boolean value) {
		PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(key, value).commit();
	}

	public static String getStringPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(key, "");
	}

	public static void setStringPrefs(Context ctx, String key, String value) {
		PreferenceManager.getDefaultSharedPreferences(ctx).edit().putString(key, value).commit();
	}

	public static int getIntPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(key, 0);
	}

	public static void setIntPrefs(Context ctx, String key, int value) {
		PreferenceManager.getDefaultSharedPreferences(ctx).edit().putInt(key, value).commit();
	}

	public static long getLongPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getLong(key, 0);
	}

	public static void setLongPrefs(Context ctx, String key, long value) {
		PreferenceManager.getDefaultSharedPreferences(ctx).edit().putLong(key, value).commit();
	}

	public static float getFloatPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getFloat(key, 0);
	}

	public static void setFloatPrefs(Context ctx, String key, float value) {
		PreferenceManager.getDefaultSharedPreferences(ctx).edit().putFloat(key, value).commit();
	}

	public static void clearPrefs(Context ctx) {
		PreferenceManager.getDefaultSharedPreferences(ctx).edit().clear().commit();
	}

	/**************************************
	 ***************** Files ***************
	 **************************************/

	public static final String APP_PATH = "/exports";

	public static File createFile(int type, String path, String name, Context ctx) throws IOException,
			ExternalStorageNotFoundException {
		if (!enabled(EXTERNAL_STORAGE, ctx)) {
			throw new ExternalStorageNotFoundException();
		}

		File storageLocation = new File(Environment.getExternalStorageDirectory(), path);

		if (!storageLocation.exists()) {
			storageLocation.mkdirs();
		}

		String extension = ".txt";

		switch (type) {
		case ExportFragment.EXPORT_EXCEL:
			extension = ".csv";
			break;
		case ExportFragment.EXPORT_PDF:
			extension = ".pdf";
			break;
		default:
			break;
		}
		File file = new File(storageLocation, name + extension);
		file.createNewFile();
		return file;
	}

	/**************************************
	 ************* Validations ************
	 **************************************/

	/**
	 * Validate hex with regular expression
	 * 
	 * @param email
	 *            email for validation
	 * @return true valid hex, false invalid hex
	 */
	public static boolean validateEmail(final String email) {
		Pattern pattern;
		Matcher matcher;

		String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

		pattern = Pattern.compile(EMAIL_PATTERN);
		matcher = pattern.matcher(email);
		return matcher.matches();
	}
}
