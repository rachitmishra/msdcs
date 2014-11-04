package in.ceeq.msdcs.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import in.ceeq.msdcs.utils.Utils;

public class SurveyProvider extends ContentProvider {

    private SurveyDatabaseHelper mDatabaseHelper;

    private SQLiteDatabase mDatabase;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int SURVEYS_MATCH = 7002;

    private static final int DETAILS_MATCH = 7003;

    private static final int USERS_MATCH = 7004;

    private static final int SURVEY_DETAILS_MATCH = 7005;

    private static final String SELECT = " SELECT ";

    private static final String AND = " AND ";

    // private static final String OR = " OR ";

    private static final String FROM = " FROM ";

    private static final String WHERE = " WHERE ";

    private static final String LEFT_JOIN = " LEFT JOIN ";

    private static final String ON = " ON ";

    // private static final String ORDER_BY = " ORDER BY ";

    // private static final String HAVING = " HAVING ";

    private static final String COMMA_SEPARATOR = ",";

    private static final String EQUAL_TO = " = ";

    // private static final String GREATER_THAN = " > ";

    // private static final String LESS_THAN = " < ";

    // private static final String GREATER_THAN_EQUAL_TO = " >= ";

    // private static final String LESS_THAN_EQUAL_TO = " =< ";

    // private static final String IS_NULL = " IS NULL ";

    // private static final String IS_NOT_NULL = " IS NOT NULL ";

    // private static final String NOT_EQUAL_TO = " != ";

    // private static final String LIKE = " LIKE ";

    public static final String ASC = " ASC ";

    //private static final String DESC = " DESC ";

    public SurveyProvider () {
    }

    @Override
    public boolean onCreate () {
        mDatabaseHelper = new SurveyDatabaseHelper(getContext());

        sUriMatcher.addURI(SurveyContract.AUTHORITY, SurveyContract.Surveys.PATH, SURVEYS_MATCH);
        sUriMatcher.addURI(SurveyContract.AUTHORITY, SurveyContract.Details.PATH, DETAILS_MATCH);
        sUriMatcher.addURI(SurveyContract.AUTHORITY, SurveyContract.Surveys.JOIN_PATH, SURVEY_DETAILS_MATCH);
        sUriMatcher.addURI(SurveyContract.AUTHORITY, SurveyContract.Users.PATH, USERS_MATCH);

        return (mDatabaseHelper == null) ? false : true;
    }

    @Override
    public String getType (Uri uri) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SURVEYS_MATCH:
                return SurveyContract.Surveys.CONTENT_TYPE;
            case USERS_MATCH:
                return SurveyContract.Users.CONTENT_TYPE;
            case DETAILS_MATCH:
                return SurveyContract.Details.CONTENT_TYPE;
            case SURVEY_DETAILS_MATCH:
                return SurveyContract.Details.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("NO FOUND: " + uri);
        }
    }

    @Override
    public Cursor query (Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder mQueryBuilder = new SQLiteQueryBuilder();
        mDatabase = mDatabaseHelper.getReadableDatabase();

        StringBuilder mQuery;
        Cursor cursor;

        int uriType = sUriMatcher.match(uri);
        switch (uriType) {
            case SURVEY_DETAILS_MATCH:
                StringBuilder tables = new StringBuilder().append(SurveyContract.Surveys.PATH).append(COMMA_SEPARATOR)
                        .append(SurveyContract.Details.PATH).append(COMMA_SEPARATOR).append(SurveyContract.Users.PATH);
                mQueryBuilder.setTables(tables.toString());
                StringBuilder where = new StringBuilder().append(Utils.getJoinColumnName(SurveyContract.Details.PATH,
                        SurveyContract.Details._ID))
                        .append(EQUAL_TO).append(Utils.getJoinColumnName(SurveyContract.Surveys.PATH,
                                SurveyContract.Surveys.DETAILS_ID))
                        .append(AND).append(Utils.getJoinColumnName(SurveyContract.Users.PATH,
                                SurveyContract.Users._ID))
                        .append(EQUAL_TO).append(Utils.getJoinColumnName(SurveyContract.Surveys.PATH,
                                SurveyContract.Surveys.USER_ID));
                if (!TextUtils.isEmpty(selection)) {
                    where.append(AND);
                    where.append(selection);
                }

                cursor = mQueryBuilder.query(mDatabase, projection, where.toString(), selectionArgs, null, null,
                        sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;
            case USERS_MATCH:
                mQueryBuilder.setTables(SurveyContract.Users.PATH);
                cursor = mQueryBuilder.query(mDatabase, projection, selection, selectionArgs, sortOrder, null, null);
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;
            default:
                throw new IllegalArgumentException("NOT FOUND: " + uri);
        }
    }

    @Override
    public Uri insert (Uri uri, ContentValues values) {
        mDatabase = mDatabaseHelper.getWritableDatabase();

        Uri result = null;

        int uriType = sUriMatcher.match(uri);
        switch (uriType) {
            case SURVEYS_MATCH:

                ContentValues detailValues = new ContentValues();
                detailValues.put(SurveyContract.Details.DATE_SOWING,
                        values.getAsLong(SurveyContract.Details.DATE_SOWING));
                detailValues.put(SurveyContract.Details.DATE_SURVEY,
                        values.getAsLong(SurveyContract.Details.DATE_SURVEY));
                detailValues.put(SurveyContract.Details.CROP_STAGE, values.getAsInteger(SurveyContract.Details
                        .CROP_STAGE));
                detailValues.put(SurveyContract.Details.DISEASE_NAME,
                        values.getAsString(SurveyContract.Details.DISEASE_NAME));
                detailValues.put(SurveyContract.Details.DISEASE_SEVERITY_SCORE,
                        values.getAsString(SurveyContract.Details.DISEASE_SEVERITY_SCORE));
                detailValues.put(SurveyContract.Details.PEST_NAME, values.getAsString(SurveyContract.Details
                        .PEST_NAME));
                detailValues.put(SurveyContract.Details.PEST_INFESTATION_COUNT,
                        values.getAsString(SurveyContract.Details.PEST_INFESTATION_COUNT));
                detailValues.put(SurveyContract.Details.LATITUDE, values.getAsDouble(SurveyContract.Details.LATITUDE));
                detailValues.put(SurveyContract.Details.LONGITUDE, values.getAsDouble(SurveyContract.Details
                        .LONGITUDE));
                detailValues.put(SurveyContract.Surveys.CREATED_ON, System.currentTimeMillis());
                detailValues.put(SurveyContract.Surveys.CREATED_BY, values.getAsString(SurveyContract.Surveys.USER_ID));

                long detailRowId = mDatabase.insert(SurveyContract.Details.PATH, null, detailValues);

                ContentValues surveyContentValues = new ContentValues();

                surveyContentValues.put(SurveyContract.Surveys.USER_ID, values.getAsLong(SurveyContract.Surveys
                        .USER_ID));
                surveyContentValues.put(SurveyContract.Surveys.DETAILS_ID, detailRowId);
                surveyContentValues.put(SurveyContract.Surveys.CREATED_ON, System.currentTimeMillis());
                surveyContentValues.put(SurveyContract.Surveys.CREATED_BY, values.getAsString(SurveyContract.Users
                        ._ID));

                long surveyRowId = mDatabase.insert(SurveyContract.Surveys.PATH, null, surveyContentValues);

                if (surveyRowId > 0) {
                    result = ContentUris.withAppendedId(SurveyContract.Surveys.CONTENT_URI, surveyRowId);
                    getContext().getContentResolver().notifyChange(result, null);
                }
                break;
            case USERS_MATCH:
                long userRowId = mDatabase.insert(SurveyContract.Users.PATH, null, values);

                if (userRowId > 0) {
                    result = ContentUris.withAppendedId(SurveyContract.Users.CONTENT_URI, userRowId);
                    getContext().getContentResolver().notifyChange(result, null);
                }
        }

        return result;
    }

    @Override
    public int update (Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int delete (Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
