package in.ceeq.msdcs.fragment;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import in.ceeq.msdcs.R;
import in.ceeq.msdcs.activity.HomeActivity;
import in.ceeq.msdcs.provider.SurveyContract;
import in.ceeq.msdcs.utils.Utils;

public class MapFragment extends Fragment implements OnMapClickListener, OnMarkerClickListener, View.OnClickListener,
		OnDateSetListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, LoaderCallbacks<Cursor> {

	public static final int SOWING_DATE_PICKER = 1;

	public static final int SURVEY_DATE_PICKER = 2;

	public static final double INDIA_LATITUDE = 21.0000;

	public static final double INDIA_LONGITUDE = 78.0000;

	private static final int LOADER_ID = 1;

	private static final String MAP_ZOOM_LEVEL = "map_zoom_level";

	private GoogleMap mMap;

	private MapView mMapView;

	private Button mSowingDate;

	private Button mSurveyDate;

	private EditText mDiseaseName;

	private EditText mDiseaseSeverityScore;

	private EditText mPestName;

	private EditText mPestInfestationCount;

	private Spinner mCropStageSpinner;

	private LinearLayout mFormLayout;

	private LinearLayout mSave;

	private LinearLayout mCancel;

	private int mCurrentDatePicker;

	private LinearLayout mMapLayout;

	private ImageButton mMapToggle;

	private LinearLayout mLocationLayout;

	private ImageButton mLocationToggle;

	private LinearLayout mZoomLayout;

	private ImageButton mZoomIn;

	private ImageButton mZoomOut;

	private LocationRequest mLocationRequest;

	private LocationClient mLocationClient;

	private Location mCurrentLocation;

    private boolean mZoomControlsEnabled;

    private int mSelectedSowingYear;

    private int mSelectedSowingMonth;

    private int mSelectedSowingDay;

    private int mSelectedSurveyYear;

    private int mSelectedSurveyMonth;

    private int mSelectedSurveyDay;


	public static MapFragment newInstance() {
		return new MapFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		mLocationClient = new LocationClient(getActivity(), this, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_map, container, false);

		setupUi(rootView);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.map_crop_stages,
				R.layout.base_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCropStageSpinner.setAdapter(adapter);
		mCropStageSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				parent.setTag(R.string.spinner_value, parent.getItemAtPosition(pos).toString());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		mMapView = (MapView) rootView.findViewById(R.id.mapView);
		mMapView.onCreate(savedInstanceState);
		mMapView.onResume();
		try {
			MapsInitializer.initialize(getActivity().getApplicationContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
		mMap = mMapView.getMap();
		setupMap();

		mLocationClient.connect();
		setupLoader(false);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
	}

	private void setupUi(View rootView) {
		Typeface typeFace = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Light.ttf");

		mFormLayout = (LinearLayout) rootView.findViewById(R.id.formLayout);
		mCropStageSpinner = (Spinner) rootView.findViewById(R.id.cropStage);

		mSowingDate = (Button) rootView.findViewById(R.id.dateOfSowing);
		mSowingDate.setTypeface(typeFace);
		mSurveyDate = (Button) rootView.findViewById(R.id.dateOfSurvey);
		mSurveyDate.setTypeface(typeFace);

		Calendar calendar = new GregorianCalendar();
		mSurveyDate.setText(Utils.getFormattedDate(System.currentTimeMillis(), "dd-MMMM-yyyy"));
		mSurveyDate.setTag(R.string.tag_year, calendar.get(Calendar.YEAR));
		mSurveyDate.setTag(R.string.tag_month, calendar.get(Calendar.MONTH));
		mSurveyDate.setTag(R.string.tag_day, calendar.get(Calendar.DAY_OF_MONTH));
		mDiseaseName = (EditText) rootView.findViewById(R.id.diseaseName);
		mDiseaseName.setTypeface(typeFace);
		mDiseaseSeverityScore = (EditText) rootView.findViewById(R.id.diseaseSeverity);
		mDiseaseSeverityScore.setTypeface(typeFace);
		mPestName = (EditText) rootView.findViewById(R.id.pestName);
		mPestName.setTypeface(typeFace);
		mPestInfestationCount = (EditText) rootView.findViewById(R.id.pestInfestationCount);
		mPestInfestationCount.setTypeface(typeFace);

		((TextView) rootView.findViewById(R.id.addLabel)).setTypeface(typeFace);
		((TextView) rootView.findViewById(R.id.dateOfSowingLabel)).setTypeface(typeFace);
		((TextView) rootView.findViewById(R.id.dateOfSurveyLabel)).setTypeface(typeFace);
		((TextView) rootView.findViewById(R.id.saveTextView)).setTypeface(typeFace);
		((TextView) rootView.findViewById(R.id.cancelTextView)).setTypeface(typeFace);
		mMapLayout = (LinearLayout) rootView.findViewById(R.id.toggleMapLayout);
		mMapToggle = (ImageButton) rootView.findViewById(R.id.toggleMap);
		mLocationLayout = (LinearLayout) rootView.findViewById(R.id.currentLocationLayout);
		mLocationToggle = (ImageButton) rootView.findViewById(R.id.currentLocation);
		mZoomLayout = (LinearLayout) rootView.findViewById(R.id.zoomLayout);
		mZoomIn = (ImageButton) rootView.findViewById(R.id.zoomIn);
		mZoomOut = (ImageButton) rootView.findViewById(R.id.zoomOut);
		mSave = (LinearLayout) rootView.findViewById(R.id.save);
		mCancel = (LinearLayout) rootView.findViewById(R.id.cancel);

		mSave.setOnClickListener(this);
		mCancel.setOnClickListener(this);
		mSowingDate.setOnClickListener(this);
		mSurveyDate.setOnClickListener(this);
		mMapToggle.setOnClickListener(this);
		mLocationToggle.setOnClickListener(this);
		mZoomIn.setOnClickListener(this);
		mZoomOut.setOnClickListener(this);
	}

	private void setupMap() {
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(INDIA_LATITUDE, INDIA_LONGITUDE), 5.0f));
		mMap.getUiSettings().setCompassEnabled(true);
		mMap.getUiSettings().setZoomControlsEnabled(false);
		mMap.getUiSettings().setMyLocationButtonEnabled(false);
		mMap.setMyLocationEnabled(true);
		mMap.setOnMapClickListener(this);
		mMap.setOnMarkerClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		mMapView.onResume();

        mZoomControlsEnabled = Utils.getBooleanPrefs(getActivity(), getActivity().getString(R.string
                .key_map_zoom_enabled));

		if (mZoomControlsEnabled) {
			mZoomLayout.setVisibility(View.VISIBLE);
		}

		if (Utils.getBooleanPrefs(getActivity(), getActivity().getString(R.string.key_map_state__save_enabled))) {
			restoreMapState();
		} else {
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(INDIA_LATITUDE, INDIA_LONGITUDE), 5.0f));
		}

		setupLoader(true);
	}

	private void restoreMapState() {
		double lastLatitude = Utils.getFloatPrefs(getActivity(), SurveyContract.Details.LATITUDE);
		double lastLongitude = Utils.getFloatPrefs(getActivity(), SurveyContract.Details.LONGITUDE);
		float lastZoom = Utils.getFloatPrefs(getActivity(), MAP_ZOOM_LEVEL);

		if (lastLatitude != 0 && lastLongitude != 0) {
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLatitude, lastLongitude), lastZoom));
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mMapView.onPause();
		saveMapState();
	}

	private void saveMapState() {
		CameraPosition currentCameraPosition = mMap.getCameraPosition();
		Utils.setFloatPrefs(getActivity(), SurveyContract.Details.LATITUDE,
				(float) currentCameraPosition.target.latitude);
		Utils.setFloatPrefs(getActivity(), SurveyContract.Details.LONGITUDE,
				(float) currentCameraPosition.target.longitude);
		Utils.setFloatPrefs(getActivity(), MAP_ZOOM_LEVEL, (float) currentCameraPosition.zoom);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
		if (mLocationClient.isConnected()) {
			mLocationClient.removeLocationUpdates(this);
		}
		mLocationClient.disconnect();
		saveMapState();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mMapView.onLowMemory();
	}

	@Override
	public void onMapClick(LatLng point) {
		((HomeActivity) getActivity()).toggleAppbar(false);
		mMapLayout.setVisibility(View.GONE);
		mLocationLayout.setVisibility(View.GONE);
        mZoomLayout.setVisibility(View.GONE);
		resetFormLayout();
		mFormLayout.setVisibility(View.VISIBLE);

		mFormLayout.setTag(R.string.tag_latitude, point.latitude);
		mFormLayout.setTag(R.string.tag_longitude, point.longitude);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		return false;
	}

	private void resetFormLayout() {
		mSowingDate.setText(getActivity().getString(R.string.choose));
		mSurveyDate.setText(Utils.getFormattedDate(System.currentTimeMillis(), "dd-MMMM-yyyy"));
		mDiseaseName.setText("");
		mDiseaseSeverityScore.setText("");
		mPestName.setText("");
		mPestInfestationCount.setText("");
		mCropStageSpinner.setSelection(-1);
	}

	@Override
	public void onClick(View v) {
		Calendar calendar = new GregorianCalendar();
		switch (v.getId()) {
		case R.id.toggleMap:
			toggleMapState();
			break;
		case R.id.currentLocation:
			setMapToCurrentLocation();
			break;
		case R.id.zoomIn:
			mMap.animateCamera(CameraUpdateFactory.zoomIn());
			break;
		case R.id.zoomOut:
			mMap.animateCamera(CameraUpdateFactory.zoomOut());
			break;
		case R.id.cancel:
			mFormLayout.setVisibility(View.GONE);
			mMapLayout.setVisibility(View.VISIBLE);
			mLocationLayout.setVisibility(View.VISIBLE);
            if (mZoomControlsEnabled) {
                mZoomLayout.setVisibility(View.VISIBLE);
            }
			((HomeActivity) getActivity()).toggleAppbar(true);
			Utils.hideKeyboard(getActivity());
			break;

		case R.id.save:

			if (!validate()) {
				Toast.makeText(getActivity(), "Entered values are not correct.", Toast.LENGTH_SHORT).show();
				return;
			}

			mFormLayout.setVisibility(View.GONE);
			mMapLayout.setVisibility(View.VISIBLE);
			mLocationLayout.setVisibility(View.VISIBLE);
            if (mZoomControlsEnabled) {
                mZoomLayout.setVisibility(View.VISIBLE);
            }
			((HomeActivity) getActivity()).toggleAppbar(true);
			Utils.hideKeyboard(getActivity());
			saveSurveyData();
			break;
		case R.id.dateOfSowing:
			mCurrentDatePicker = SOWING_DATE_PICKER;
			new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH)).show();
			break;
		case R.id.dateOfSurvey:
			mCurrentDatePicker = SURVEY_DATE_PICKER;
			new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH)).show();
			break;
		}
	}

	private void saveSurveyData() {
		ContentValues surveyValues = new ContentValues();

		Calendar sowingDateCalendar = new GregorianCalendar();
        Calendar surveyDateCalendar = new GregorianCalendar();

        sowingDateCalendar.set((Integer) mSowingDate.getTag(R.string.tag_year),
                ((Integer) mSowingDate.getTag(R.string.tag_month)), (Integer) mSowingDate.getTag(R.string.tag_day));

		surveyValues.put(SurveyContract.Details.DATE_SOWING, sowingDateCalendar.getTimeInMillis());

        surveyDateCalendar.set((Integer) mSurveyDate.getTag(R.string.tag_year),
                ((Integer) mSurveyDate.getTag(R.string.tag_month)), (Integer) mSurveyDate.getTag(R.string.tag_day));

		surveyValues.put(SurveyContract.Details.DATE_SURVEY, surveyDateCalendar.getTimeInMillis());

		surveyValues.put(SurveyContract.Details.DISEASE_NAME, mDiseaseName.getText().toString());
		surveyValues.put(SurveyContract.Details.DISEASE_SEVERITY_SCORE, mDiseaseSeverityScore.getText().toString());
		surveyValues.put(SurveyContract.Details.PEST_NAME, mPestName.getText().toString());
		surveyValues.put(SurveyContract.Details.PEST_INFESTATION_COUNT, mPestInfestationCount.getText().toString());
		surveyValues.put(SurveyContract.Details.CROP_STAGE, mCropStageSpinner.getSelectedItemPosition());
		double latitude = (Double) mFormLayout.getTag(R.string.tag_latitude);
		double longitude = (Double) mFormLayout.getTag(R.string.tag_longitude);
		surveyValues.put(SurveyContract.Details.LATITUDE, latitude);
		surveyValues.put(SurveyContract.Details.LONGITUDE, longitude);
		surveyValues.put(SurveyContract.Surveys.USER_ID, Utils.getCurrentUser(getActivity()).mId + "");

		NewSurveyQuery.newInstance(getActivity().getContentResolver(), getActivity(), mMap).startInsert(0,
				surveyValues, SurveyContract.Surveys.CONTENT_URI, surveyValues);
	}

	private boolean validate() {
		boolean valid = true;

		if (mSowingDate.getTag(R.string.tag_year) == null) {
			valid = false;
			mSowingDate.setError("Sowing date cannot be empty.");
		}

		return valid;
	}

	private static class NewSurveyQuery extends AsyncQueryHandler {

		private Context mContext;
		private GoogleMap mMap;

		public static NewSurveyQuery newInstance(ContentResolver contentResolver, Context context, GoogleMap map) {
			return new NewSurveyQuery(contentResolver, context, map);
		}

		public NewSurveyQuery(ContentResolver contentResolver, Context context, GoogleMap map) {
			super(contentResolver);
			this.mContext = context;
			this.mMap = map;
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			Toast.makeText(mContext, "Survey saved successfully.", Toast.LENGTH_SHORT).show();
			ContentValues surveyValues = (ContentValues) cookie;
			mMap.addMarker(new MarkerOptions()
					.draggable(true)
					.position(
							new LatLng(surveyValues.getAsDouble(SurveyContract.Details.LATITUDE), surveyValues
									.getAsDouble(SurveyContract.Details.LONGITUDE)))
					.title(Utils.getFormattedDate(surveyValues.getAsLong(SurveyContract.Details.DATE_SURVEY),
							"dd-MMMM-yyyy"))
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_place)));
		}
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		switch (mCurrentDatePicker) {
		case SOWING_DATE_PICKER:
            mSowingDate.setTag(R.string.tag_year, year);
            mSowingDate.setTag(R.string.tag_month, monthOfYear);
            mSowingDate.setTag(R.string.tag_day, dayOfMonth);
			mSowingDate.setText(String.format("%02d", dayOfMonth) + "-"
					+ new DateFormatSymbols().getMonths()[monthOfYear] + "-" + year);
			break;
		case SURVEY_DATE_PICKER:
            mSurveyDate.setTag(R.string.tag_year, year);
            mSurveyDate.setTag(R.string.tag_month, monthOfYear);
            mSurveyDate.setTag(R.string.tag_day, dayOfMonth);
			mSurveyDate.setText(String.format("%02d", dayOfMonth) + "-"
					+ new DateFormatSymbols().getMonths()[monthOfYear] + "-" + year);
			break;
		}
	}

	@Override
	public void onLocationChanged(Location newLocation) {
		mCurrentLocation = newLocation;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {

	}

	@Override
	public void onConnected(Bundle arg0) {
		mCurrentLocation = mLocationClient.getLastLocation();
		setMapToCurrentLocation();
	}

	@Override
	public void onDisconnected() {
	}

	private void toggleMapState() {
		switch (mMap.getMapType()) {
		case GoogleMap.MAP_TYPE_NORMAL:
			mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			break;
		case GoogleMap.MAP_TYPE_TERRAIN:
			mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			break;
		case GoogleMap.MAP_TYPE_SATELLITE:
			mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		case GoogleMap.MAP_TYPE_HYBRID:
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			break;
		}
	}

	private void setMapToCurrentLocation() {
		if (mCurrentLocation != null) {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(),
					mCurrentLocation.getLongitude()), 15));
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity(), SurveyContract.Surveys.JOIN_CONTENT_URI,
				SurveyContract.Surveys.DETAILED_PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		if (cursor != null && cursor.getCount() > 0) {
			int latitudeIndex = cursor.getColumnIndex(SurveyContract.Details.LATITUDE);
			int longitudeIndex = cursor.getColumnIndex(SurveyContract.Details.LONGITUDE);
			int surveyDateIndex = cursor.getColumnIndex(SurveyContract.Details.DATE_SURVEY);

			while (cursor.moveToNext()) {
				mMap.addMarker(new MarkerOptions()
						.draggable(true)
						.position(new LatLng(cursor.getDouble(latitudeIndex), cursor.getDouble(longitudeIndex)))
						.title(new SimpleDateFormat("dd-MMMM-yyyy", Locale.getDefault()).format(cursor
								.getLong(surveyDateIndex)))
						.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_place)));
			}
		}
	}

	public void setupLoader(boolean mReset) {
		if (mReset) {
			getLoaderManager().restartLoader(LOADER_ID, null, this);
			return;
		}
		getLoaderManager().initLoader(LOADER_ID, null, this);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

}