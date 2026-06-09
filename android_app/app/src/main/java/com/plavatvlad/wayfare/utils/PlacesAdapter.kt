import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Place
import com.plavatvlad.wayfare.utils.PlaceImagesAdapter

class PlacesAdapter(
    private var items: List<Place>,
    private val onPlaceClick: (Place) -> Unit
) : RecyclerView.Adapter<PlacesAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.placeTitle)
        val snippet: TextView = view.findViewById(R.id.placeSnippet)
        val rating: TextView = view.findViewById(R.id.placeRating)
        val imagesRecycler: RecyclerView = view.findViewById(R.id.imagesRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.place_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val place = items[position]

        holder.title.text = place.name
        holder.snippet.text = place.description.split("\n").firstOrNull().orEmpty()
        holder.rating.text = "⭐ ${"%.1f".format(place.ratingAverage)} (${place.ratingCount})"

        holder.imagesRecycler.apply {
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = PlaceImagesAdapter(
                images = place.photoUrls,
                canManageImages = false,
                onLongClick = {}
            )
        }
        holder.itemView.setOnClickListener {
            onPlaceClick(place)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Place>) {
        items = newItems
        notifyDataSetChanged()
    }
}