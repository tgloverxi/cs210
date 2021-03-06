package ca.ubc.cs.cpsc210.translink.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.R;
import ca.ubc.cs.cpsc210.translink.model.Bus;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.Stop;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A plotter for bus stop locations
public class BusStopPlotter extends MapViewOverlay {
    /** clusterer */
    private RadiusMarkerClusterer stopClusterer;
    /** maps each stop to corresponding marker on map */
    private Map<Stop, Marker> stopMarkerMap = new HashMap<>();
    /** marker for stop that is nearest to user (null if no such stop) */
    private Marker nearestStnMarker;
    private Activity activity;
    private StopInfoWindow stopInfoWindow;

    /**
     * Constructor
     * @param activity  the application context
     * @param mapView  the map view on which buses are to be plotted
     */
    public BusStopPlotter(Activity activity, MapView mapView) {
        super(activity.getApplicationContext(), mapView);
        this.activity = activity;
        nearestStnMarker = null;
        stopInfoWindow = new StopInfoWindow((StopSelectionListener) activity, mapView);
        newStopClusterer();
    }

    public RadiusMarkerClusterer getStopClusterer() {
        return stopClusterer;
    }

    /**
     * Mark all visible stops in stop manager onto map.
     */
    public void markStops(Location currentLocation) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);

        updateVisibleArea();
        newStopClusterer();

        for(Stop next :  StopManager.getInstance()){
            if (Geometry.rectangleContainsPoint(northWest,southEast,next.getLocn())){
                Marker stopMarker = new Marker(mapView);
                stopMarker.setRelatedObject(next);

                stopMarker.setIcon(stopIconDrawable);
                GeoPoint geoPoint = new GeoPoint(next.getLocn().getLatitude(),next.getLocn().getLongitude());
                stopMarker.setPosition(geoPoint);
                String busNumber = String.valueOf(next.getNumber());

                String stringOfNumber = "";
                for (Route route: next.getRoutes()){
                    stringOfNumber = stringOfNumber.concat("\r\n" + route.getNumber());
                }
                    stopMarker.setTitle(busNumber+" "+next.getName()+ stringOfNumber);

                stopMarker.setInfoWindow(this.stopInfoWindow);
                setMarker(next,stopMarker);
                stopClusterer.add(stopMarker);
            }
        }

         if (nearestStnMarker != null || currentLocation != null){
            updateMarkerOfNearest((Stop) nearestStnMarker.getRelatedObject());
            }


    }


    /**
     * Create a new stop cluster object used to group stops that are close by to reduce screen clutter
     */
    private void newStopClusterer() {
        stopClusterer = new RadiusMarkerClusterer(activity);
        stopClusterer.getTextPaint().setTextSize(20.0F * BusesAreUs.dpiFactor());
        int zoom =  mapView == null ? 16 : mapView.getZoomLevel();
        if (zoom == 0) zoom = MapDisplayFragment.DEFAULT_ZOOM;
        int radius = 1000 / zoom;

        stopClusterer.setRadius(radius);
        Drawable clusterIconD = activity.getResources().getDrawable(R.drawable.stop_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        stopClusterer.setIcon(clusterIcon);
    }

    /**
     * Update marker of nearest stop (called when user's location has changed).  If nearest is null,
     * no stop is marked as the nearest stop.
     *
     * @param nearest   stop nearest to user's location (null if no stop within StopManager.RADIUS metres)
     */
    public void updateMarkerOfNearest(Stop nearest) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);
        Drawable closestStopIconDrawable = activity.getResources().getDrawable(R.drawable.closest_stop_icon);

        if (nearestStnMarker != null && !(nearestStnMarker.getRelatedObject().equals(nearest))){
            nearestStnMarker.setIcon(stopIconDrawable);
        }

        if (nearest != null){
            if (getMarker(nearest) != null){
                getMarker(nearest).setIcon(closestStopIconDrawable);
                nearestStnMarker = getMarker(nearest);
            }
            if (getMarker(nearest) == null){
                nearestStnMarker = new Marker(mapView);;
                GeoPoint geoPoint = new GeoPoint(nearest.getLocn().getLatitude(),nearest.getLocn().getLongitude());
                nearestStnMarker.setPosition(geoPoint);
                stopClusterer.add(nearestStnMarker);

                setMarker(nearest,nearestStnMarker);
                nearestStnMarker.setRelatedObject(nearest);
                nearestStnMarker.setIcon(closestStopIconDrawable);

                nearestStnMarker.setInfoWindow(this.stopInfoWindow);
                String busNumber = String.valueOf(nearest.getNumber());
                nearestStnMarker.setTitle(busNumber+" "+nearest.getName());
            }
        }

        else {nearestStnMarker = null;}


    }

    /**
     * Manage mapping from stops to markers using a map from stops to markers.
     * The mapping in the other direction is done using the Marker.setRelatedObject() and
     * Marker.getRelatedObject() methods.
     */
    private Marker getMarker(Stop stop) { return stopMarkerMap.get(stop); }
    private void setMarker(Stop stop, Marker marker) { stopMarkerMap.put(stop, marker); }
    private void clearMarker(Stop stop) { stopMarkerMap.remove(stop); }
    private void clearMarkers() { stopMarkerMap.clear(); }
}
