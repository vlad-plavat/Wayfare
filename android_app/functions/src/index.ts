import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

admin.initializeApp();
export * from "./feed";

export const updatePlaceRating = onDocumentWritten(
  "places/{placeId}/reviews/{userId}",
  async (event) => {
    const placeId = event.params.placeId;

    const reviewsSnapshot = await admin
      .firestore()
      .collection("places")
      .doc(placeId)
      .collection("reviews")
      .get();

    let total = 0;

    reviewsSnapshot.forEach((doc) => {
      total += Number(doc.data().rating ?? 0);
    });

    const count = reviewsSnapshot.size;
    const average = count > 0 ? total / count : 0;

    await admin
      .firestore()
      .collection("places")
      .doc(placeId)
      .update({
        ratingAverage: average,
        ratingCount: count,
      });
  }
);
