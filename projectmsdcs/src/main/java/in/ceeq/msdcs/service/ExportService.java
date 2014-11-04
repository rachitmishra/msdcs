package in.ceeq.msdcs.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.johnpersano.supertoasts.SuperToast;

import in.ceeq.msdcs.fragment.ExportFragment;
import in.ceeq.msdcs.provider.SurveyContract;
import in.ceeq.msdcs.provider.SurveyProvider;
import in.ceeq.msdcs.utils.Utils;

public class ExportService extends IntentService {

    private String mFileName;

    private int mExportType;

    public ExportService () {
        super("ExportService");
    }

    @Override
    protected void onHandleIntent (Intent intent) {

        // Get bundled data to service.
        Bundle exportParams = intent.getExtras();
        mFileName = exportParams.getString(ExportFragment.EXPORT_FILE_NAME);
        mExportType = exportParams.getInt(ExportFragment.EXPORT_FILE_TYPE);

        // Little show off.
        SuperToast.create(this, "Exporting data ...", Toast.LENGTH_LONG).show();

        // Get data from server
        Cursor exportDataCursor = getContentResolver().query(SurveyContract.Surveys.JOIN_CONTENT_URI,
                SurveyContract.Surveys.DETAILED_PROJECTION, null, null, Utils.getJoinColumnName(SurveyContract.Details
                                .PATH,
                        SurveyContract.Details
                                ._ID) + SurveyProvider.ASC);

        // Call export method to export data
        try {
            if (exportDataCursor != null && exportDataCursor.getCount() > 0) {
                Utils.export(mExportType, this, mFileName, exportDataCursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }

        // And again little show off.
        SuperToast.create(this, "Data export complete.", Toast.LENGTH_LONG).show();
    }
}
