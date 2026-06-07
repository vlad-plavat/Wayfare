const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.updatePlaceRating = onDocumentWritten(
  "places/{placeId}/reviews/{userId}",
  async (event) => {

    const placeId = event.params.placeId;

    const reviews = await admin.firestore()
      .collection("places")
      .doc(placeId)
      .collection("reviews")
      .get();

    let total = 0;

    reviews.forEach(doc => {
      total += doc.data().rating || 0;
    });

    const count = reviews.size;
    const average = count > 0 ? total / count : 0;

    await admin.firestore()
      .collection("places")
      .doc(placeId)
      .update({
        averageRating: average,
        reviewCount: count
      });
  }
);