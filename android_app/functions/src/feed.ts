import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// -------------------------
// Types
// -------------------------

interface Place {
  id: string;
  ratingAverage?: number;
  ratingCount?: number;
  latitude?: number;
  longitude?: number;
  createdAt?: any;
  createdBy?: string;
  publicAvailable?: boolean;
  [key: string]: any;
}

// -------------------------
// Helpers
// -------------------------

function distance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371;

  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);

  return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

function ratingScore(p: Place): number {
  return p.ratingAverage || 0;
}

// -------------------------
// Main Function
// -------------------------

export const getPlacesFeed = onCall(async (request) => {
  const strategy = request.data.strategy || "default";
  const uid = request.data.uid as string | undefined;

  const lat = Number(request.data.lat ?? 0);
  const lng = Number(request.data.lng ?? 0);

  if (!uid) {
    throw new Error("Missing UID");
  }

  const locationAvailable = lat !== 0 && lng !== 0;

  // -------------------------
  // Fetch places
  // -------------------------

  const [publicSnap, privateSnap] = await Promise.all([
    db.collection("places")
      .where("publicAvailable", "==", true)
      .get(),
    db.collection("places")
      .where("createdBy", "==", uid)
      .get()
  ]);

  // Merge + dedupe
  const map = new Map<string, Place>();

  [...publicSnap.docs, ...privateSnap.docs].forEach((doc) => {
    map.set(doc.id, { id: doc.id, ...doc.data() } as Place);
  });

  let places: Place[] = Array.from(map.values());

  // -------------------------
  // STRATEGIES
  // -------------------------

  switch (strategy) {
    case "rating":
      places.sort(
        (a, b) => ratingScore(b) - ratingScore(a)
      );
      break;

    case "distance":
      if (!locationAvailable) {
        places.sort(
          (a, b) => ratingScore(b) - ratingScore(a)
        );
        break;
      }

      places = places.map((p) => {
        const dist = distance(
          lat,
          lng,
          p.latitude || 0,
          p.longitude || 0
        );

        const distanceScore = 1 / (dist + 1);

        return {
          ...p,
          _score:
            ratingScore(p) * 0.4 +
            distanceScore * 0.6
        };
      });

      places.sort(
        (a, b) => (b._score || 0) - (a._score || 0)
      );
      break;

    case "product": {
      const seed = uid
        .split("")
        .reduce((a, c) => a + c.charCodeAt(0), 0);

      places = places.map((p) => {
        const createdAtMs =
          p.createdAt?.toMillis?.() ??
          p.createdAt ??
          0;

        const ageDays =
          (Date.now() - createdAtMs) / 86400000;

        const freshnessScore = 1 / (ageDays + 1);

        const randomBoost =
          ((seed % 100) / 100) * 0.3;

        return {
          ...p,
          _score:
            ratingScore(p) * 0.5 +
            freshnessScore * 0.3 +
            randomBoost * 0.2
        };
      });

      places.sort(
        (a, b) => (b._score || 0) - (a._score || 0)
      );
      break;
    }

    default:
      places.sort(
        (a, b) => ratingScore(b) - ratingScore(a)
      );
      break;
  }

  // -------------------------
  // RESPONSE
  // -------------------------

  return {
    placeIds: places.map((p) => p.id)
  };
});