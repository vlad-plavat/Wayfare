import android.content.Context
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Place
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class PlaceManager(
    private val map: MapView,
    private val context: Context
) {

    fun addPlaceMarker(place: Place) {
        val marker = Marker(map)

        marker.position = GeoPoint(place.latitude,place.longitude)

        marker.title = place.name
        marker.snippet = place.description
        marker.relatedObject = place.id
        marker.icon = ContextCompat.getDrawable(
            context,
            R.drawable.ic_place_marker
        )

        map.overlays.add(marker)
    }

    fun loadPlaces() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val query = db.collection("places")
            .where(
                Filter.or(
                    Filter.equalTo("isPublic", true),
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
    }
}