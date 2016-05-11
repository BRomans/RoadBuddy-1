package it.unitn.roadbuddy.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.PointOfInterest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity
        extends AppCompatActivity
        implements OnMapReadyCallback {

    TextView mTapTextView;
    TextView mCameraTextView;
    GoogleMap map;
    LinearLayout lyMapButtons;
    FrameLayout mainFrameLayout;
    NFA nfa;
    View currentMenuBar;
    Map<Marker, PointOfInterest> shownPOIs;
    PointOfInterest selectedPOI;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        try {
            Class.forName( "org.postgresql.Driver" );  // FIXME [ed] find a better place
        }
        catch ( ClassNotFoundException e ) {
            Log.e( getClass( ).getName( ), "backend exception", e );
            showToast( "could not load postgres jdbc driver" );
            finish( );
        }

        mTapTextView = ( TextView ) findViewById( R.id.tap_text );
        mCameraTextView = ( TextView ) findViewById( R.id.camera_text );
        lyMapButtons = ( LinearLayout ) findViewById( R.id.lyMapButtons );
        mainFrameLayout = ( FrameLayout ) findViewById( R.id.mainFrameLayout );
        shownPOIs = new HashMap<>( );

        SupportMapFragment mapFragment =
                ( SupportMapFragment ) getSupportFragmentManager( ).findFragmentById( R.id.map );

        mapFragment.getMapAsync( this );
    }

    @Override
    public void onMapReady( GoogleMap map ) {
        this.map = map;
        nfa = new NFA( this, new RestState( ) );
    }

    public void RefreshMapContent( ) {
        LatLngBounds bounds = map.getProjection( ).getVisibleRegion( ).latLngBounds;
        new RefreshMapAsync( ).executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, bounds );
    }

    public View setCurrentMenuBar( int view ) {
        View v = getLayoutInflater( ).inflate( view, mainFrameLayout, false );
        setCurrentMenuBar( v );

        return currentMenuBar;
    }

    public void setCurrentMenuBar( View v ) {
        removeMenuBar( );
        currentMenuBar = v;
        mainFrameLayout.addView( v );
    }

    public void removeMenuBar( ) {
        if ( currentMenuBar != null )
            mainFrameLayout.removeView( currentMenuBar );
        currentMenuBar = null;
    }

    public void showMenuBar( ) {
        if ( currentMenuBar != null )
            currentMenuBar.setVisibility( View.VISIBLE );
    }

    public void hideMenuBar( ) {
        if ( currentMenuBar != null )
            currentMenuBar.setVisibility( View.INVISIBLE );
    }

    public void toggleMenuBar( ) {
        if ( currentMenuBar != null ) {
            if ( currentMenuBar.getVisibility( ) == View.VISIBLE )
                currentMenuBar.setVisibility( View.INVISIBLE );
            else
                currentMenuBar.setVisibility( View.VISIBLE );
        }
    }

    public void showToast( String text ) {
        Toast.makeText( getApplicationContext( ), text, Toast.LENGTH_LONG ).show( );
    }

    public void showToast( int textId ) {
        String msg = getString( textId );
        showToast( msg );
    }

    class RefreshMapAsync extends AsyncTask<LatLngBounds, Integer, List<PointOfInterest>> {

        String exceptionMessage;

        @Override
        protected List<PointOfInterest> doInBackground( LatLngBounds... bounds ) {
            try {
                return DAOFactory.getPoiDAOFactory( ).getPOIsInside(
                        getApplicationContext( ), bounds[ 0 ]
                );
            }
            catch ( BackendException e ) {
                Log.e( "roadbuddy", "backend exception", e );
                exceptionMessage = e.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( List<PointOfInterest> points ) {
            if ( points != null ) {
                /**
                 * [ed] overwrite all POIs with fresh data coming from the database, also
                 * redraw all of them except for the currently selected POI
                 *
                 * TODO do not redraw existing POIs, just update their data.
                 * I suspect this would case noticeable flicker
                 */
                for ( Map.Entry<Marker, PointOfInterest> entry : shownPOIs.entrySet( ) ) {
                    if ( entry.getValue( ) != selectedPOI ) {
                        entry.getKey( ).remove( );
                    }
                }

                shownPOIs.clear( );
                for ( PointOfInterest p : points ) {
                    Marker marker;
                    if ( p != selectedPOI ) {
                        marker = p.drawToMap( map );
                    }
                    else {
                        marker = selectedPOI.getMarker( );
                        p.setMarker( marker );
                    }
                    shownPOIs.put( marker, p );
                }
            }
            else if ( exceptionMessage != null ) {
                showToast( exceptionMessage );
            }
            else {
                showToast( R.string.generic_backend_error );
            }
        }
    }
}
