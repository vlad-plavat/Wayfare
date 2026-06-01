import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Place
import com.plavatvlad.wayfare.ui.PlaceDetailsEdit
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.plavatvlad.wayfare.ui.PlaceDetailsFragment

class PlaceManager(
    private val map: MapView,
    private val context: Context,
    private val fragmentManager: FragmentManager
) {

    fun addPlaceMarker(place: Place) {
        val marker = Marker(map)

        marker.position = GeoPoint(place.latitude,place.longitude)

        marker.title = place.name
        marker.snippet = place.description
        marker.relatedObject = place.id
        val drawable = (
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_place_marker
                )?.mutate()
                ) as? GradientDrawable

        drawable?.setColor(
            ContextCompat.getColor(
                context,
                if (place.publicAvailable)
                    R.color.public_marker
                else
                    R.color.private_marker
            )
        )

        marker.icon = drawable
        marker.infoWindow = null
        marker.setOnMarkerClickListener { m, _ ->
            val id = m.relatedObject as String
            openPlaceDetails(id)
            true
        }

        map.overlays.add(marker)
    }

    fun loadPlaces() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val query = db.collection("places")
            .where(
                Filter.or(
                    Filter.equalTo("publicAvailable", true),
                    Filter.equalTo("createdBy", uid)
                )
            )

        query.get()
            .addOnSuccessListener { result ->

                map.overlays.removeAll { it is Marker  && it.relatedObject != null}

                val places = result.documents.mapNotNull { doc ->
                    doc.toObject(Place::class.java)
                }

                places.forEach { place ->
                    addPlaceMarker(place)
                }
            }
    }

    fun savePlace(place: Place) {
        FirebaseFirestore.getInstance()
            .collection("places")
            .document(place.id)
            .set(place)
        addPlaceMarker(place)
    }

    private fun openPlaceDetails(placeId: String) {

        val activity = map.context as AppCompatActivity
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("places")
            .document(placeId)
            .get()
            .addOnSuccessListener { doc ->

                val createdBy = doc.getString("createdBy")
                val fragment = if (createdBy == uid) {
                    PlaceDetailsEdit.newInstance(placeId)
                } else {
                    PlaceDetailsFragment.newInstance(placeId)
                }

                activity.supportFragmentManager
                    .beginTransaction()
                    .add(R.id.overlay_container, fragment)
                    .addToBackStack("place_details")
                    .commit()
            }
    }
}