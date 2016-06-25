package it.unitn.roadbuddy.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;
import java.util.List;


public class TripsFragment extends Fragment implements SearchView.OnQueryTextListener {

    public static final String INTENT_SELECTED_TRIP = "select-trip";
    final static String PATHS_LIST_KEY = "path-list";
    MainActivity mPActivity;
    ViewPager mPager;
    PagerAdapter mPagerAdapter;
    CancellableAsyncTaskManager taskManager;
    EmptyRecyclerView mRecyclerView;
    TripsAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    View emptyView;
    View tripsView;
    double latitude;
    double longitude;
    LatLng latLng;
    Location myLocation;
    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private SearchView searchView;
    private List<Path> resList;

    public TripsFragment( ) {
        // Required empty public constructor
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        this.mPActivity = ( MainActivity ) getActivity( );
        super.onCreate( savedInstanceState );

        // ShakeDetector initialization
        mSensorManager = ( SensorManager ) getActivity( ).getSystemService( Context.SENSOR_SERVICE );
        mAccelerometer = mSensorManager
                .getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
        mShakeDetector = new ShakeDetector( );
        mShakeDetector.setOnShakeListener( new ShakeDetector.OnShakeListener( ) {

            @Override
            public void onShake( int count ) {
                /*
                 * The following method, "handleShakeEvent(count):" is a stub //
				 * method you would use to setup whatever you want done once the
				 * device has been shook.
				 */
                handleShakeEvent( );
            }
        } );

    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        // Inflate the layout for this fragment
        tripsView = inflater.inflate( R.layout.fragment_trips, container, false );

        final SearchView searchView = ( SearchView ) tripsView.findViewById( R.id.action_search );
        searchView.setOnQueryTextListener( this );

        return tripsView;

    }

    @Override
    public void onViewCreated( View view, @Nullable Bundle savedInstanceState ) {

        super.onViewCreated( view, savedInstanceState );
        //Setting the recycler view
        this.mPager = ( ViewPager ) getActivity( ).findViewById( R.id.pager );
        this.mRecyclerView = ( EmptyRecyclerView ) view.findViewById( R.id.recycler_view );
        this.emptyView = view.findViewById( R.id.empty_view );
        mRecyclerView.setEmptyView( emptyView );
        this.mPagerAdapter = ( PagerAdapter ) mPager.getAdapter( );

        // use a linear layout manager
        this.mLayoutManager = new LinearLayoutManager( getContext( ) );
        mRecyclerView.setLayoutManager( mLayoutManager );

        latitude = 46.00;
        longitude = 11.00;

        this.taskManager = new CancellableAsyncTaskManager( );
        if ( ActivityCompat.checkSelfPermission( getActivity( ), Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            //Get Location Manager object for System Service LOCATION_SERVICE
            LocationManager locationManager = ( LocationManager ) getActivity( ).getSystemService( Context.LOCATION_SERVICE );

            //Create a new Criteria to retrieve provider
            Criteria criteria = new Criteria( );

            //Get the name of the best provider
            String provider = locationManager.getBestProvider( criteria, true );

            //Get current user location
            myLocation = locationManager.getLastKnownLocation( provider );

            if ( myLocation != null ) {
                //Get latitude
                latitude = myLocation.getLatitude( );
                //Get longitude
                longitude = myLocation.getLongitude( );
            }

        }
        //Create a LatLng object for the current user's location
        latLng = new LatLng( latitude, longitude );

        taskManager.startRunningTask( new getTrips( getContext( ) ), true, latLng );

        if ( savedInstanceState != null ) {
            resList = savedInstanceState.getParcelableArrayList( PATHS_LIST_KEY );
            mAdapter = new TripsAdapter( resList );
            mRecyclerView.setAdapter( mAdapter );
        }
    }

    @Override
    public void onPause( ) {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener( mShakeDetector );
        super.onPause( );
        taskManager.stopRunningTasksOfType( getTrips.class );
        updateList( );
    }

    @Override
    public void onAttach( Context context ) {
        super.onAttach( context );
    }

    @Override
    public void onDetach( ) {
        super.onDetach( );
    }

    @Override
    public void onResume( ) {
        super.onResume( );
        updateList( );
        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener( mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI );
        Log.v( "MY_STATE_LOG", "contenuto ricaricato" );
    }

    @Override
    public void onDestroy( ) {
        super.onDestroy( );
        Log.v( "MY_STATE_LOG", "trips fragment distrutto" );
    }

    public void updateList( ) {
        LatLng myPos;
        if ( myLocation != null ) {
            //Get latitude
            latitude = myLocation.getLatitude( );
            //Get longitude
            longitude = myLocation.getLongitude( );
        }
        //Create a LatLng object for the current user's location
        myPos = new LatLng( latitude, longitude );
        taskManager.startRunningTask( new getTrips( getContext( ) ), true, myPos );
    }

    public void handleShakeEvent( ) {
        Log.v( "SHAKE", "Device shaked" );
        showToast( "Trip list updating.." );
        updateList( );
    }

    public void showToast( String text ) {
        Toast.makeText( getActivity( ).getApplicationContext( ), text, Toast.LENGTH_SHORT ).show( );
    }

    public void showToast( int textId ) {
        String msg = getString( textId );
        showToast( msg );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        ArrayList<Path> paths = new ArrayList<>( );
        if ( resList != null ) {
            for ( Path p : resList )
                paths.add( p );
        }
        outState.putParcelableArrayList( PATHS_LIST_KEY, paths );
    }

    @Override
    public boolean onQueryTextChange( String query ) {

        final List<Path> filteredPathList = filter( resList, query );
        if ( mAdapter != null ) {
            mAdapter.animateTo( filteredPathList );
            mRecyclerView.scrollToPosition( 0 );
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit( String query ) {
        final List<Path> filteredPathList = filter( resList, query );
        mAdapter.animateTo( filteredPathList );
        mRecyclerView.scrollToPosition( 0 );
        return true;
    }

    private List<Path> filter( List<Path> paths, String query ) {
        query = query.toLowerCase( );

        final List<Path> filteredPathList = new ArrayList<>( );
        for ( Path path : paths ) {
            final String text = path.getDescription( ).toLowerCase( );
            if ( text.contains( query ) ) {
                filteredPathList.add( path );
            }
        }

        return filteredPathList;


    }

    public interface ClickListener {
        void onClick( View view, int position );

        void onLongClick( View view, int position );
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private TripsFragment.ClickListener clickListener;

        public RecyclerTouchListener( Context context, final RecyclerView recyclerView, final TripsFragment.ClickListener clickListener ) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector( context, new GestureDetector.SimpleOnGestureListener( ) {
                @Override
                public boolean onSingleTapUp( MotionEvent e ) {
                    return true;
                }

                @Override
                public void onLongPress( MotionEvent e ) {
                    View child = recyclerView.findChildViewUnder( e.getX( ), e.getY( ) );
                    if ( child != null && clickListener != null ) {
                        clickListener.onLongClick( child, recyclerView.getChildPosition( child ) );
                    }
                }
            } );
        }

        @Override
        public boolean onInterceptTouchEvent( RecyclerView rv, MotionEvent e ) {

            View child = rv.findChildViewUnder( e.getX( ), e.getY( ) );
            if ( child != null && clickListener != null && gestureDetector.onTouchEvent( e ) ) {
                clickListener.onClick( child, rv.getChildPosition( child ) );
            }
            return false;
        }

        @Override
        public void onTouchEvent( RecyclerView rv, MotionEvent e ) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent( boolean disallowIntercept ) {

        }
    }

    class getTrips extends CancellableAsyncTask<LatLng, Integer, List<Path>> {

        String exceptionMessage;
        Context context;

        public getTrips( Context context ) {
            super( taskManager );
            this.context = context;
        }

        @Override
        protected List<Path> doInBackground( LatLng... pos ) {

            try {
                List<Path> paths = DAOFactory.getPathDAO( ).getPathsFromPosition(
                        context, pos[ 0 ]
                );
                return paths;
            }
            catch ( BackendException e ) {
                Log.e( getClass( ).getName( ), "while getting trips from position", e );
                exceptionMessage = e.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( List<Path> res ) {

            //sending data to the recycler view
            resList = res;
            if ( res == null )
                return;

            mAdapter = new TripsAdapter( resList );
            mRecyclerView.setAdapter( mAdapter );

            mRecyclerView.addOnItemTouchListener( new RecyclerTouchListener( getContext( ), mRecyclerView, new ClickListener( ) {
                @Override
                public void onClick( View view, int position ) {

                    //sending data from the recycler view to the sliderLayout
                    Path path = mAdapter.getPath( position );
                    mPActivity.showChoosenPath( path );
                    /*Intent intent = new Intent( getContext(), MainActivity.class);
                    intent.setAction(INTENT_SELECTED_TRIP);
                    Bundle savedInstanceState;
                    //intent.putExtra(INTENT_SELECTED_TRIP, path);
                    intent.putExtra(INTENT_SELECTED_TRIP, path.getId());
                    startActivity(intent);*/


                }

                @Override
                public void onLongClick( View view, int position ) {
                    showToast( "Shake to update list" );
                }
            } ) );
            super.onPostExecute( res );
        }
    }
}
